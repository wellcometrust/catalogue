package uk.ac.wellcome.platform.transformer.calm.services

import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import org.scalatest.EitherValues
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.work.internal.{IdentifierType, Work, WorkState}
import uk.ac.wellcome.pipeline_storage.{PipelineStorageStream, Retriever}
import uk.ac.wellcome.platform.transformer.calm.CalmRecord
import uk.ac.wellcome.platform.transformer.calm.generators.CalmRecordGenerators
import uk.ac.wellcome.storage.generators.S3ObjectLocationGenerators
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.memory.MemoryTypedStore
import weco.catalogue.source_model.CalmSourcePayload
import weco.catalogue.transformer.{
  TransformerWorker,
  TransformerWorkerTestCases
}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CalmTransformerWorkerTest
    extends TransformerWorkerTestCases[
      MemoryTypedStore[S3ObjectLocation, CalmRecord],
      CalmSourcePayload,
      CalmRecord]
    with EitherValues
    with CalmRecordGenerators
    with S3ObjectLocationGenerators {

  override def withContext[R](
    testWith: TestWith[MemoryTypedStore[S3ObjectLocation, CalmRecord], R]): R =
    testWith(
      MemoryTypedStore[S3ObjectLocation, CalmRecord](initialEntries = Map.empty)
    )

  override def createId: String = UUID.randomUUID().toString

  override def createPayloadWith(id: String, version: Int)(
    implicit store: MemoryTypedStore[S3ObjectLocation, CalmRecord])
    : CalmSourcePayload = {
    val record = createCalmRecordWith(
      "Title" -> "abc",
      "Level" -> "Collection",
      "RefNo" -> "a/b/c",
      "AltRefNo" -> "a.b.c",
      "CatalogueStatus" -> "Catalogued"
    )

    val location = createS3ObjectLocation

    store.put(location)(record.copy(id = id)) shouldBe a[Right[_, _]]

    CalmSourcePayload(id = id, location = location, version = version)
  }

  override def setPayloadVersion(p: CalmSourcePayload, version: Int)(
    implicit store: MemoryTypedStore[S3ObjectLocation, CalmRecord])
    : CalmSourcePayload = {
    val storedData: CalmRecord = store.get(p.location).value.identifiedT

    val location = createS3ObjectLocation
    store.put(location)(storedData) shouldBe a[Right[_, _]]

    p.copy(location = location, version = version)
  }

  override def createBadPayload(
    implicit store: MemoryTypedStore[S3ObjectLocation, CalmRecord])
    : CalmSourcePayload =
    CalmSourcePayload(
      id = UUID.randomUUID().toString,
      location = createS3ObjectLocation,
      version = 1)

  override implicit val encoder: Encoder[CalmSourcePayload] =
    deriveConfiguredEncoder[CalmSourcePayload]

  override def assertMatches(p: CalmSourcePayload, w: Work[WorkState.Source])(
    implicit store: MemoryTypedStore[S3ObjectLocation, CalmRecord]): Unit = {
    w.sourceIdentifier.identifierType shouldBe IdentifierType("calm-record-id")
    w.sourceIdentifier.value shouldBe p.id
  }

  override def withWorker[R](
    pipelineStream: PipelineStorageStream[NotificationMessage,
                                          Work[WorkState.Source],
                                          String],
    retriever: Retriever[Work[WorkState.Source]])(
    testWith: TestWith[TransformerWorker[CalmSourcePayload, CalmRecord, String],
                       R])(
    implicit recordReadable: MemoryTypedStore[S3ObjectLocation, CalmRecord])
    : R =
    testWith(
      new CalmTransformerWorker(
        pipelineStream = pipelineStream,
        recordReadable = recordReadable,
        retriever = retriever
      )
    )
}
