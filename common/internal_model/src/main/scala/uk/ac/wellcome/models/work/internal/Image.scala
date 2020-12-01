package uk.ac.wellcome.models.work.internal

import java.time.Instant

case class ImageData[+State](
  id: State,
  version: Int,
  locations: List[DigitalLocationDeprecated]
) extends HasId[State]

case class Image[State <: ImageState](
  version: Int,
  state: State,
  locations: List[DigitalLocationDeprecated]
) {
  def id: String = state.id
  def sourceIdentifier: SourceIdentifier = state.sourceIdentifier

  def transition[OutState <: ImageState](args: OutState#TransitionArgs = ())(
    implicit transition: ImageFsm.Transition[State, OutState])
    : Image[OutState] =
    Image[OutState](
      state = transition.state(state, args),
      version = version,
      locations = locations
    )
}

sealed trait ImageState {
  type TransitionArgs

  val sourceIdentifier: SourceIdentifier

  def id: String = sourceIdentifier.toString
}

/** ImageState represents the state of the image in the pipeline.
  * Its stages are as follows:

  *      |
  *      | (merger)
  *      ▼
  *    Merged
  *      |
  *      | (image id minter)
  *      ▼
  *  Identified
  *      |
  *      | (inferrer)
  *      ▼
  *  Augmented
  *
  */
object ImageState {

  case class Merged(
    sourceIdentifier: SourceIdentifier,
    modifiedTime: Instant,
    source: ImageSource[DataState.Unidentified]
  ) extends ImageState {
    type TransitionArgs = Unit
  }

  case class Identified(
    sourceIdentifier: SourceIdentifier,
    canonicalId: String,
    modifiedTime: Instant,
    source: ImageSource[DataState.Identified]
  ) extends ImageState {
    type TransitionArgs = Unit

    override def id = canonicalId
  }

  case class Augmented(
    sourceIdentifier: SourceIdentifier,
    canonicalId: String,
    modifiedTime: Instant,
    source: ImageSource[DataState.Identified],
    inferredData: Option[InferredData] = None
  ) extends ImageState {
    type TransitionArgs = Option[InferredData]

    override def id = canonicalId
  }

}

// ImageFsm contains all of the possible transitions between image states
object ImageFsm {
  import ImageState._

  sealed trait Transition[InState <: ImageState, OutState <: ImageState] {
    def state(state: InState, args: OutState#TransitionArgs): OutState
  }

  implicit val identifiedToAugmented = new Transition[Identified, Augmented] {
    def state(state: Identified,
              inferredData: Option[InferredData]): Augmented =
      Augmented(
        sourceIdentifier = state.sourceIdentifier,
        canonicalId = state.canonicalId,
        modifiedTime = state.modifiedTime,
        source = state.source,
        inferredData = inferredData
      )
  }
}
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
