package uk.ac.wellcome.platform.transformer.calm

import uk.ac.wellcome.bigmessaging.BigMessageSender
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.storage.{Identified, ReadError}
import uk.ac.wellcome.storage.store.VersionedStore
import uk.ac.wellcome.typesafe.Runnable
import uk.ac.wellcome.transformer.common.worker.{Transformer, TransformerWorker}

class CalmTransformerWorker(
  val stream: SQSStream[NotificationMessage],
  val sender: BigMessageSender[SNSConfig],
  store: VersionedStore[String, Int, CalmRecord],
) extends Runnable
    with TransformerWorker[CalmRecord, SNSConfig] {

  val transformer: Transformer[CalmRecord] = CalmTransformer

  override protected def lookupSourceData(key: StoreKey): Either[ReadError, Identified[StoreKey, CalmRecord]] =
    store.getLatest(key.id)
}
