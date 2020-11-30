package uk.ac.wellcome.platform.transformer.calm

import uk.ac.wellcome.bigmessaging.BigMessageSender
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.work.internal.Work
import uk.ac.wellcome.models.work.internal.WorkState.Source
import uk.ac.wellcome.pipeline_storage.ElasticIndexer
import uk.ac.wellcome.storage.{Identified, ReadError}
import uk.ac.wellcome.storage.store.VersionedStore
import uk.ac.wellcome.typesafe.Runnable
import uk.ac.wellcome.transformer.common.worker.{Transformer, TransformerWorker}

import scala.concurrent.ExecutionContext

class CalmTransformerWorker(
  val stream: SQSStream[NotificationMessage],
  val sender: BigMessageSender[SNSConfig],
  val workIndexer: ElasticIndexer[Work[Source]],
  store: VersionedStore[String, Int, CalmRecord],
)(
  implicit val ec: ExecutionContext
) extends Runnable
    with TransformerWorker[CalmRecord, SNSConfig] {

  val transformer: Transformer[CalmRecord] = CalmTransformer

  override protected def lookupSourceData(
    key: StoreKey): Either[ReadError, CalmRecord] =
    store
      .getLatest(key.id)
      .map { case Identified(_, calmRecord) => calmRecord }
}
