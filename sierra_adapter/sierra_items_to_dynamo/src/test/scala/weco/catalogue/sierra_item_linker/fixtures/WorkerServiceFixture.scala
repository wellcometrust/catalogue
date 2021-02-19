package weco.catalogue.sierra_item_linker.fixtures

import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.monitoring.Metrics
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.sierra_adapter.model.SierraItemNumber
import uk.ac.wellcome.storage.store.memory.MemoryVersionedStore
import weco.catalogue.sierra_item_linker.linker.{
  SierraItemLink,
  SierraItemLinkStore,
  SierraItemLinkerWorkerService
}

import scala.concurrent.Future

trait WorkerServiceFixture extends SQS with Akka {

  def withWorkerService[R](
    queue: Queue,
    store: MemoryVersionedStore[SierraItemNumber, SierraItemLink] =
      MemoryVersionedStore[SierraItemNumber, SierraItemLink](
        initialEntries = Map.empty),
    metrics: Metrics[Future] = new MemoryMetrics(),
    messageSender: MemoryMessageSender = new MemoryMessageSender
  )(testWith: TestWith[SierraItemLinkerWorkerService[String], R]): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue, metrics) { sqsStream =>
        val workerService = new SierraItemLinkerWorkerService[String](
          sqsStream = sqsStream,
          linkStore = new SierraItemLinkStore(store),
          messageSender = messageSender
        )

        workerService.run()

        testWith(workerService)
      }
    }
}
