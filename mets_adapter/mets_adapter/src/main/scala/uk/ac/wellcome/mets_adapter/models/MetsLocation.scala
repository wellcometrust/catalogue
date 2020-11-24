package uk.ac.wellcome.mets_adapter.models

import java.time.Instant

import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

/** METS location data to send onwards to the transformer.
  */
case class MetsLocation(
  bucket: String,
  path: String,
  version: Int,
  file: String,
  createdDate: Instant,
  manifestations: List[String] = Nil,
  // If Digital Production want to "delete" a bag, they send a new version
  // through Goobi that doesn't have any files.
  //
  // The bag will contain a METS file, and nothing else.  e.g. b1665836x v3
  //
  // This flag records whether a bag only contains the METS file, which is
  // a clue to us that we should ignore it.
  isOnlyMets: Boolean = false
) {

  private def s3Prefix = S3ObjectLocationPrefix(bucket, keyPrefix = path)

  def xmlLocation: S3ObjectLocation = s3Prefix.asLocation(file)

  def manifestationLocations: List[S3ObjectLocation] =
    manifestations.map { s3Prefix.asLocation(_) }
}
