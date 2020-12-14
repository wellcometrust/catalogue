package weco.catalogue.source_model

import uk.ac.wellcome.storage.s3.S3ObjectLocation
import weco.catalogue.source_model.mets.MetsSourceData

sealed trait SourcePayload {
  val id: String
  val version: Int
}

case class CalmSourcePayload(
  id: String,
  location: S3ObjectLocation,
  version: Int
) extends SourcePayload

case class MiroInventorySourcePayload(
  id: String,
  location: S3ObjectLocation,
  version: Int
) extends SourcePayload

case class MiroSourcePayload(
  id: String,
  isClearedForCatalogueAPI: Boolean,
  location: S3ObjectLocation,
  version: Int
) extends SourcePayload

case class MetsSourcePayload(
  id: String,
  sourceData: MetsSourceData,
  version: Int
) extends SourcePayload

case class SierraSourcePayload(
  id: String,
  location: S3ObjectLocation,
  version: Int
) extends SourcePayload