package weco.pipeline.transformer

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Source
import weco.catalogue.source_model.CalmSourcePayload
import weco.fixtures.TestWith
import weco.json.JsonUtil._
import weco.messaging.fixtures.SQS.{Queue, QueuePair}
import weco.messaging.memory.MemoryMessageSender
import weco.pipeline.transformer.example.{
  ExampleData,
  ExampleTransformerWorker,
  ValidExampleData
}
import weco.pipeline_storage.fixtures.PipelineStorageStreamFixtures
import weco.pipeline_storage.memory.{MemoryIndexer, MemoryRetriever}
import weco.storage.Version
import weco.storage.generators.S3ObjectLocationGenerators
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.memory.MemoryVersionedStore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TransformerWorkerTest
    extends AnyFunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with PipelineStorageStreamFixtures
    with IdentifiersGenerators
    with S3ObjectLocationGenerators {

  it("if it can't look up the source data, it fails") {
    withLocalSqsQueuePair(visibilityTimeout = 1.second) {
      case QueuePair(queue, dlq) =>
        withWorker(queue) { _ =>
          sendNotificationToSQS(queue, Version("A", 1))

          eventually {
            assertQueueHasSize(dlq, size = 1)
            assertQueueEmpty(queue)
          }
        }
    }
  }

  it("uses the version from the store, not the message") {
    val storeVersion = 5
    val messageVersion = storeVersion - 1

    val location = createS3ObjectLocation
    val data = ValidExampleData(
      id = createSourceIdentifier,
      title = randomAlphanumeric()
    )

    val sourceStore = MemoryVersionedStore[S3ObjectLocation, ExampleData](
      initialEntries = Map(
        Version(location, storeVersion) -> data
      )
    )

    val payload = CalmSourcePayload(
      id = data.id.toString,
      location = location,
      version = messageVersion
    )

    val workIndexer = new MemoryIndexer[Work[Source]]()

    withLocalSqsQueue() { queue =>
      withWorker(queue, workIndexer = workIndexer, sourceStore = sourceStore) {
        _ =>
          sendNotificationToSQS(queue, payload)

          eventually {
            workIndexer.index.values.map { _.version }.toSeq shouldBe Seq(
              storeVersion)
          }
      }
    }
  }

  def withWorker[R](
    queue: Queue,
    workIndexer: MemoryIndexer[Work[Source]] = new MemoryIndexer[Work[Source]](),
    workKeySender: MemoryMessageSender = new MemoryMessageSender(),
    sourceStore: MemoryVersionedStore[S3ObjectLocation, ExampleData] =
      MemoryVersionedStore[S3ObjectLocation, ExampleData](
        initialEntries = Map.empty)
  )(
    testWith: TestWith[Unit, R]
  ): R =
    withPipelineStream[Work[Source], R](
      queue = queue,
      indexer = workIndexer,
      sender = workKeySender) { pipelineStream =>
      val worker = new ExampleTransformerWorker(
        pipelineStream = pipelineStream,
        sourceStore = sourceStore,
        retriever = new MemoryRetriever[Work[Source]](index = workIndexer.index)
      )

      worker.run()

      testWith(())
    }
}
