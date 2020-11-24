package uk.ac.wellcome.mets_adapter.models

import java.time.Instant

import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

/** METS location data to send onwards to the transformer.
  */
case class MetsLocation(bucket: String,
                        path: String,
                        version: Int,
                        file: String,
                        createdDate: Instant,
                        manifestations: List[String] = Nil) {

  private def s3Prefix = S3ObjectLocationPrefix(bucket, keyPrefix = path)

  def xmlLocation: S3ObjectLocation = s3Prefix.asLocation(file)

  def manifestationLocations: List[S3ObjectLocation] =
    manifestations.map { s3Prefix.asLocation(_) }
}
