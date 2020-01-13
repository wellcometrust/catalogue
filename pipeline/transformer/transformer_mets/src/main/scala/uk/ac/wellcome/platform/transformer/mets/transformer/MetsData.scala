package uk.ac.wellcome.platform.transformer.mets.transformer

import uk.ac.wellcome.models.work.internal._

import cats.implicits._
import org.apache.commons.lang3.StringUtils.equalsIgnoreCase

case class MetsData(
  recordIdentifier: String,
  accessConditionDz: Option[String] = None,
  accessConditionStatus: Option[String] = None,
  accessConditionUsage: Option[String] = None,
  thumbnailLocation: Option[String] = None
) {

  def toWork(version: Int): Either[Throwable, UnidentifiedInvisibleWork] =
    for {
      maybeLicense <- parseLicense
      accessStatus <- parseAccessStatus
      unidentifiableItem: MaybeDisplayable[Item] = Unidentifiable(
        Item(locations = List(digitalLocation(maybeLicense, accessStatus))))
    } yield
      UnidentifiedInvisibleWork(
        version = version,
        sourceIdentifier = sourceIdentifier,
        workData(unidentifiableItem, thumbnail(maybeLicense))
      )

  private def workData(unidentifiableItem: MaybeDisplayable[Item],
                       thumbnail: Option[DigitalLocation]) =
    WorkData(
      items = List(unidentifiableItem),
      mergeCandidates = List(mergeCandidate),
      thumbnail = thumbnail,
    )

  private def mergeCandidate = MergeCandidate(
    identifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Work",
      value = recordIdentifier
    ),
    reason = Some("METS work")
  )

  private def digitalLocation(license: Option[License], accessStatus: Option[AccessStatus]) =
    DigitalLocation(
      url = s"https://wellcomelibrary.org/iiif/$recordIdentifier/manifest",
      locationType = LocationType("iiif-presentation"),
      license = license,
      accessConditions = accessStatus.map { status =>
        List(AccessCondition(status = status, terms = accessConditionUsage))
      }
    )

  // The access conditions in mets contains sometimes the license id (lowercase),
  // sometimes the label (ie "in copyright")
  // and sometimes the url of the license
  private def parseLicense: Either[Exception, Option[License]] =
    accessConditionDz.map { accessCondition =>
      License.values.find { license =>
        equalsIgnoreCase(license.id, accessCondition) || equalsIgnoreCase(
          license.label,
          accessCondition) || license.url.equals(accessCondition)
      } match {
        case Some(license) => Right(license)
        case None =>
          Left(new Exception(s"Couldn't match $accessCondition to a license"))
      }
    }.sequence

  private val parseAccessStatus: Either[Exception, Option[AccessStatus]] =
    accessConditionStatus
      .map(_.toLowerCase)
      .map {
        case "open" => Right(AccessStatus.Open)
        case "requires registation" => Right(AccessStatus.OpenWithAdvisory)
        case "restricted" => Right(AccessStatus.Restricted)
        case "in copyright" => Right(AccessStatus.LicensedResources)
        case status => Left(new Exception(s"Unrecognised access status: $status"))
      }
      .sequence

  private def sourceIdentifier =
    SourceIdentifier(
      IdentifierType("mets"),
      ontologyType = "Work",
      value = recordIdentifier)

  private val thumbnailDim = "200"

  private def thumbnail(maybeLicense: Option[License]) =
    thumbnailLocation.map { location =>
      DigitalLocation(
        url =
          s"https://dlcs.io/thumbs/wellcome/5/$location/full/!$thumbnailDim,$thumbnailDim/0/default.jpg",
        locationType = LocationType("thumbnail-image"),
        license = maybeLicense
      )
    }
}
