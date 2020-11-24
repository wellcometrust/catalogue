package uk.ac.wellcome.platform.router

import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.Eventually
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.WorkGenerators
import uk.ac.wellcome.models.work.internal.WorkState.Denormalised
import uk.ac.wellcome.models.work.internal.{CollectionPath, Relations, Work}
import uk.ac.wellcome.pipeline_storage.{
  ElasticRetriever,
  Indexer,
  MemoryIndexer,
  PipelineStorageConfig,
  PipelineStorageStream
}

import scala.collection.mutable
import scala.concurrent.Future

class RouterWorkerServiceTest
    extends AnyFunSpec
    with WorkGenerators
    with SQS
    with Akka
    with ElasticsearchFixtures
    with Eventually {

  it("sends collectionPath to paths topic") {
    val work = mergedWork().collectionPath(CollectionPath("a"))
    val indexer = new MemoryIndexer[Work[Denormalised]](
      mutable.Map[String, Work[Denormalised]]())
    withWorkerService(indexer) {
      case (
          mergedIndex,
          QueuePair(queue, dlq),
          worksMessageSender,
          pathsMessageSender) =>
        insertIntoElasticsearch(mergedIndex, work)
        sendNotificationToSQS(queue = queue, body = work.id)
        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
          pathsMessageSender.messages.map(_.body) should contain("a")
          worksMessageSender.messages shouldBe empty
          indexer.index shouldBe empty
        }
    }
  }

  it("sends a work without collectionPath to works topic") {
    val work = mergedWork()
    val indexer = new MemoryIndexer[Work[Denormalised]](
      mutable.Map[String, Work[Denormalised]]())
    withWorkerService(indexer) {
      case (
          mergedIndex,
          QueuePair(queue, dlq),
          worksMessageSender,
          pathsMessageSender) =>
        insertIntoElasticsearch(mergedIndex, work)
        sendNotificationToSQS(queue = queue, body = work.id)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
          worksMessageSender.messages.map(_.body) should contain(work.id)
          pathsMessageSender.messages shouldBe empty
          indexer.index should contain(
            work.id -> work.transition[Denormalised](Relations.none))
        }
    }
  }

  it("sends on an invisible work") {
    val work = mergedWork().collectionPath(CollectionPath("a/2")).invisible()

    val indexer = new MemoryIndexer[Work[Denormalised]](
      mutable.Map[String, Work[Denormalised]]())
    withWorkerService(indexer) {
      case (
          mergedIndex,
          QueuePair(queue, dlq),
          worksMessageSender,
          pathsMessageSender) =>
        insertIntoElasticsearch(mergedIndex, work)
        sendNotificationToSQS(queue = queue, body = work.id)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
          worksMessageSender.messages shouldBe empty
          pathsMessageSender.messages.map(_.body) should contain("a/2")
          indexer.index shouldBe empty
        }
    }
  }

  it(
    "sends the message to the dlq and doesn't send anything on if elastic indexing fails") {
    val work = mergedWork()
    val failingIndexer = new Indexer[Work[Denormalised]] {
      override def init(): Future[Unit] = Future.successful(())
      override def index(documents: Seq[Work[Denormalised]])
        : Future[Either[Seq[Work[Denormalised]], Seq[Work[Denormalised]]]] =
        Future.successful(Left(documents))
    }
    withWorkerService(failingIndexer) {
      case (
          mergedIndex,
          QueuePair(queue, dlq),
          worksMessageSender,
          pathsMessageSender) =>
        insertIntoElasticsearch(mergedIndex, work)
        sendNotificationToSQS(queue = queue, body = work.id)

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          worksMessageSender.messages shouldBe empty
          pathsMessageSender.messages shouldBe empty
        }
    }
  }

  def withWorkerService[R](indexer: Indexer[Work[Denormalised]])(
    testWith: TestWith[(Index,
                        QueuePair,
                        MemoryMessageSender,
                        MemoryMessageSender),
                       R]) = withActorSystem { implicit as =>
    implicit val es = as.dispatcher
    withLocalSqsQueuePair(visibilityTimeout = 1) {
      case q @ QueuePair(queue, _) =>
        val worksMessageSender = new MemoryMessageSender
        val pathsMessageSender = new MemoryMessageSender
        withSQSStream[NotificationMessage, R](queue) { stream =>
          val pipelineStream = new PipelineStorageStream(
            messageStream = stream,
            documentIndexer = indexer,
            messageSender = worksMessageSender)(
            PipelineStorageConfig(1, 1 second, 10))
          withLocalMergedWorksIndex { mergedIndex =>
            val service =
              new RouterWorkerService(
                pathsMsgSender = pathsMessageSender,
                workRetriever = new ElasticRetriever(elasticClient, mergedIndex),
                pipelineStream = pipelineStream
              )
            service.run()
            testWith((mergedIndex, q, worksMessageSender, pathsMessageSender))

          }
        }
    }
  }
}
