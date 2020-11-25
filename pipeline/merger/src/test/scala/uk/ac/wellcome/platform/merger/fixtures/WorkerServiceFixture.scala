package uk.ac.wellcome.platform.merger.fixtures

import scala.collection.mutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.monitoring.Metrics
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.platform.merger.services._
import uk.ac.wellcome.pipeline_storage.{MemoryIndexer, MemoryRetriever}
import uk.ac.wellcome.models.work.internal._
import WorkState.{Merged, Source}

trait WorkerServiceFixture extends LocalWorksVhs with SQS with Akka {
  def withWorkerService[R](workRetriever: MemoryRetriever[Work[Source]],
                           queue: Queue,
                           workSender: MemoryMessageSender,
                           imageSender: MemoryMessageSender =
                             new MemoryMessageSender(),
                           metrics: Metrics[Future] = new MemoryMetrics,
                           index: Map[String, Work[Merged]] = Map.empty())(
    testWith: TestWith[MergerWorkerService[String, String], R]): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue, metrics) { sqsStream =>
        val workerService = new MergerWorkerService(
          sqsStream = sqsStream,
          workRetriever = workRetriever,
          mergerRules = PlatformMerger,
          workIndexer = new MemoryIndexer(index),
          workSender = workSender,
          imageSender = imageSender
        )

        workerService.run()

        testWith(workerService)
      }
    }

  def withWorkerService[R](workRetriever: MemoryRetriever[Work[Source]])(
    testWith: TestWith[MergerWorkerService[String, String], R]): R =
    withLocalSqsQueue() { queue =>
      val workSender = new MemoryMessageSender()
      val imageSender = new MemoryMessageSender()

      withWorkerService(workRetriever, queue, workSender, imageSender) { workerService =>
        testWith(workerService)
      }
    }

  def getWorksSent(workSender: MemoryMessageSender): Seq[String] =
    workSender.messages.map { _.body }

  def getImagesSent(imageSender: MemoryMessageSender)
    : Seq[MergedImage[DataState.Unidentified]] =
    imageSender.getMessages[MergedImage[DataState.Unidentified]]
}
