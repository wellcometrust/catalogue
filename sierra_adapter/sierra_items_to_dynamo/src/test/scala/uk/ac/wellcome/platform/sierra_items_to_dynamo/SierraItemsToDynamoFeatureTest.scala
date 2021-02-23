package uk.ac.wellcome.platform.sierra_items_to_dynamo

import org.scalatest.EitherValues
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.sierra_adapter.model.Implicits._
import uk.ac.wellcome.sierra_adapter.model.{
  SierraGenerators,
  SierraItemNumber,
  SierraItemRecord
}
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.store.memory.MemoryVersionedStore
import weco.catalogue.sierra_linker.fixtures.WorkerFixture
import weco.catalogue.sierra_linker.models.Link

class SierraItemsToDynamoFeatureTest
    extends AnyFunSpec
    with Matchers
    with Eventually
    with EitherValues
    with IntegrationPatience
    with SierraAdapterHelpers
    with SierraGenerators
    with WorkerFixture {

  it("reads items from SQS, stores the link, and sends the record onward") {
    val messageSender = new MemoryMessageSender

    val record = createSierraItemRecordWith(
      bibIds = List(createSierraBibNumber)
    )

    val expectedLink = Link(record)

    val store = MemoryVersionedStore[SierraItemNumber, Link](
      initialEntries = Map.empty
    )

    withLocalSqsQueue() { queue =>
      withWorker(queue, store = store, messageSender = messageSender) {
        _ =>
          sendNotificationToSQS(queue, record)

          eventually {
            messageSender.getMessages[SierraItemRecord] shouldBe Seq(record)

            store.getLatest(record.id).value.identifiedT shouldBe expectedLink
          }
      }
    }
  }
}
