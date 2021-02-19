package weco.catalogue.sierra_item_linker.linker

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.model.{SierraItemNumber, SierraItemRecord}
import weco.catalogue.sierra_adapter.linker.SierraLinkerWorkerService

class SierraItemLinkerWorkerService[Destination](
  sqsStream: SQSStream[NotificationMessage],
  linkStore: SierraItemLinkStore,
  messageSender: MessageSender[Destination]
) extends SierraLinkerWorkerService[SierraItemNumber, SierraItemRecord, SierraItemLink, Destination](sqsStream, linkStore, messageSender)
