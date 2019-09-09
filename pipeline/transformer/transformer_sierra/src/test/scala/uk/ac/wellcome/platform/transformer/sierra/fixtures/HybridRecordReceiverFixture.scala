package uk.ac.wellcome.platform.transformer.sierra.fixtures

import scala.util.Random
import com.amazonaws.services.sns.AmazonSNS
import io.circe.Json

import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.sierra.services.{
  HybridRecord,
  HybridRecordReceiver
}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.json.JsonUtil._

import uk.ac.wellcome.models.transformable.SierraTransformable

import uk.ac.wellcome.bigmessaging.fixtures.VHSFixture
import uk.ac.wellcome.bigmessaging.EmptyMetadata
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}

import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.store.HybridStoreEntry
import uk.ac.wellcome.storage.Version

trait HybridRecordReceiverFixture extends VHSFixture[SierraTransformable] {

  def withHybridRecordReceiver[R](vhs: VHS,
                                  topic: Topic,
                                  bucket: Bucket,
                                  snsClient: AmazonSNS = snsClient)(
    testWith: TestWith[HybridRecordReceiver[SNSConfig], R]): R =
    withSqsBigMessageSender[TransformedBaseWork, R](bucket, topic, snsClient) {
      msgSender =>
        val recorderReciver = new HybridRecordReceiver(msgSender, vhs)
        testWith(recorderReciver)
    }

  def createHybridRecordNotificationWith(
    sierraTransformable: SierraTransformable,
    vhs: VHS,
    version: Int = 1): NotificationMessage = {

    val hybridRecord = createHybridRecordWith(
      sierraTransformable,
      vhs,
      version = version
    )
    createNotificationMessageWith(
      message = hybridRecord
    )
  }

  def createHybridRecordWith(
    sierraTransformable: SierraTransformable,
    vhs: VHS,
    version: Int = 1,
    id: String = Random.alphanumeric take 10 mkString): HybridRecord = {

    vhs.put(Version(id, version))(
      HybridStoreEntry(sierraTransformable, EmptyMetadata()))
    HybridRecord(
      id = id,
      version = version,
      location = Json.obj(
        ("namespace", Json.fromString("namespace.doesnt.matter")),
        ("key", Json.fromString("path/is/irrelevant"))
      )
    )
  }
}
