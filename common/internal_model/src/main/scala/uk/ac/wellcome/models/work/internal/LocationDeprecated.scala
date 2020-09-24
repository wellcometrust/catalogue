package uk.ac.wellcome.models.work.internal

sealed trait LocationDeprecated {
  val locationType: LocationType
  val accessConditions: List[AccessCondition]

  def isRestrictedOrClosed =
    accessConditions.exists { accessCondition =>
      accessCondition.status match {
        case Some(AccessStatus.Restricted) => true
        case Some(AccessStatus.Closed)     => true
        case _                             => false
      }
    }
}

case class DigitalLocationDeprecated(
  url: String,
  locationType: LocationType,
  license: Option[License] = None,
  credit: Option[String] = None,
  accessConditions: List[AccessCondition] = Nil,
  ontologyType: String = "DigitalLocation"
) extends LocationDeprecated

case class PhysicalLocationDeprecated(
  locationType: LocationType,
  label: String,
  accessConditions: List[AccessCondition] = Nil,
  ontologyType: String = "PhysicalLocation"
) extends LocationDeprecated

sealed trait LocationTypeAggregation
object LocationTypeAggregation {
  case object Digital extends LocationTypeAggregation
  case object Physical extends LocationTypeAggregation
}
