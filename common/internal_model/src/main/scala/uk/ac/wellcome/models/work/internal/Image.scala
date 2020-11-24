package uk.ac.wellcome.models.work.internal

import java.time.Instant

sealed trait BaseImage[+State <: DataState] extends HasId[State#Id] {
  val id: State#Id
  val locations: List[DigitalLocationDeprecated]
}

case class UnmergedImage[State <: DataState](
  id: State#Id,
  version: Int,
  locations: List[DigitalLocationDeprecated]
) extends BaseImage[State] {
  def mergeWith(canonicalWork: SourceWork[State],
                redirectedWork: Option[SourceWork[State]] = None,
                modifiedTime: Instant): MergedImage[State] =
    MergedImage[State](
      id = id,
      version = version,
      modifiedTime = modifiedTime,
      locations = locations,
      source = SourceWorks[State](canonicalWork, redirectedWork)
    )

  def mergeWith(source: ImageSource[State],
                modifiedTime: Instant): MergedImage[State] =
    MergedImage[State](
      id = id,
      version = version,
      modifiedTime = modifiedTime,
      locations = locations,
      source = source
    )
}

case class MergedImage[State <: DataState](
  id: State#Id,
  version: Int,
  modifiedTime: Instant,
  locations: List[DigitalLocationDeprecated],
  source: ImageSource[State]
) extends BaseImage[State]

object MergedImage {
  implicit class IdentifiedMergedImageOps(
    mergedImage: MergedImage[DataState.Identified]) {
    def augment(inferredData: => Option[InferredData]): AugmentedImage =
      AugmentedImage(
        id = mergedImage.id,
        version = mergedImage.version,
        modifiedTime = mergedImage.modifiedTime,
        locations = mergedImage.locations,
        source = mergedImage.source,
        inferredData = inferredData
      )
  }
}

case class AugmentedImage(
  id: IdState.Identified,
  version: Int,
  modifiedTime: Instant,
  locations: List[DigitalLocationDeprecated],
  source: ImageSource[DataState.Identified],
  inferredData: Option[InferredData] = None
) extends BaseImage[DataState.Identified]

case class InferredData(
  // We split the feature vector so that it can fit into
  // ES's dense vector type (max length 2048)
  features1: List[Float],
  features2: List[Float],
  lshEncodedFeatures: List[String],
  palette: List[String]
)

object InferredData {
  def empty: InferredData = InferredData(Nil, Nil, Nil, Nil)
}

object UnmergedImage {
  def apply(sourceIdentifier: SourceIdentifier,
            version: Int,
            locations: List[DigitalLocationDeprecated])
    : UnmergedImage[DataState.Unidentified] =
    UnmergedImage[DataState.Unidentified](
      id = IdState.Identifiable(sourceIdentifier),
      version = version,
      locations
    )
}
