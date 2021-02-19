package weco.catalogue.sierra_item_linker

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.sierra_adapter.model.Implicits._
import uk.ac.wellcome.sierra_adapter.model.{SierraGenerators, SierraItemRecord}
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import weco.catalogue.sierra_item_linker.fixtures.ItemLinkerWorkerServiceFixture

class SierraItemsToDynamoFeatureTest
    extends AnyFunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with SierraAdapterHelpers
    with SierraGenerators
    with ItemLinkerWorkerServiceFixture {

  it("reads items from Sierra and adds them to DynamoDB") {
    val messageSender = new MemoryMessageSender

    withLocalSqsQueue() { queue =>
      withWorkerService(queue, messageSender = messageSender) { _ =>
        val itemRecord = createSierraItemRecordWith(
          bibIds = List(createSierraBibNumber)
        )

        sendNotificationToSQS(
          queue = queue,
          message = itemRecord
        )

        eventually {
          messageSender.getMessages[SierraItemRecord] shouldBe Seq(itemRecord)
        }
      }
    }
  }
}
