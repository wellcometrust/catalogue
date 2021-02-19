package weco.catalogue.sierra_adapter.linker

import akka.Done
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.model.SierraRecordNumber
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.Future
import scala.util.{Success, Try}

class SierraLinkerWorkerService[Id <: SierraRecordNumber, Record, Link, Destination](
  sqsStream: SQSStream[NotificationMessage],
  linkStore: SierraLinkStore[Id, Record, Link],
  messageSender: MessageSender[Destination]
)(
  implicit
  decoder: Decoder[Record],
  encoder: Encoder[Record]
) extends Runnable {

  private def process(message: NotificationMessage): Try[Unit] =
    for {
      receivedRecord <- fromJson[Record](message.body)
      linkedRecord <- linkStore.update(receivedRecord).toTry
      _ <- linkedRecord match {
        case Some(r) => messageSender.sendT(r)
        case None    => Success(())
      }
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, message => Future.fromTry(process(message)))
}
