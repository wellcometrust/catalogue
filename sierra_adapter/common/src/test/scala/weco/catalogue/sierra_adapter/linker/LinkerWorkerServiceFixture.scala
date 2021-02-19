package weco.catalogue.sierra_adapter.linker

import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.monitoring.Metrics
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.sierra_adapter.model.{
  AbstractSierraRecord,
  SierraTypedRecordNumber
}
import uk.ac.wellcome.storage.store.memory.MemoryVersionedStore

import scala.concurrent.Future

trait LinkerWorkerServiceFixture[Id <: SierraTypedRecordNumber, Record <: AbstractSierraRecord[Id], Link <: SierraLink] extends SQS with Akka {
  val linker: SierraLinker[Record, Link]

  def withWorkerService[R](
    queue: Queue,
    store: MemoryVersionedStore[Id, Link] =
      MemoryVersionedStore[Id, Link](initialEntries = Map.empty),
    metrics: Metrics[Future] = new MemoryMetrics(),
    messageSender: MemoryMessageSender = new MemoryMessageSender
  )(testWith: TestWith[SierraLinkerWorkerService[Id, Record, Link, String], R])(
    implicit decoder: Decoder[Record], encoder: Encoder[Record]
  ): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue, metrics) { sqsStream =>
        val linkStore = new SierraLinkStore(store, linker)

        val workerService = new SierraLinkerWorkerService(
          sqsStream = sqsStream,
          linkStore = linkStore,
          messageSender = messageSender
        )

        workerService.run()

        testWith(workerService)
      }
    }
}
