package weco.catalogue.internal_model.image

import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  HasId,
  SourceIdentifier
}
import weco.catalogue.internal_model.locations.DigitalLocation

import java.time.Instant

case class ImageData[+State](
  id: State,
  version: Int,
  locations: List[DigitalLocation]
) extends HasId[State]

case class Image[State <: ImageState](
  version: Int,
  state: State,
  locations: List[DigitalLocation],
  source: ImageSource,
  modifiedTime: Instant
) {
  def id: String = state.id
  def sourceIdentifier: SourceIdentifier = state.sourceIdentifier

  def transition[OutState <: ImageState](args: OutState#TransitionArgs = ())(
    implicit transition: ImageFsm.Transition[State, OutState]
  ): Image[OutState] =
    Image[OutState](
      state = transition.state(this, args),
      version = version,
      locations = locations,
      source = source,
      modifiedTime = modifiedTime
    )
}

sealed trait ImageState {
  type TransitionArgs

  val canonicalId: CanonicalId
  val sourceIdentifier: SourceIdentifier

  def id: String = canonicalId.toString
}

/** ImageState represents the state of the image in the pipeline.
  * Its stages are as follows:

  *      |
  *      | (merger)
  *      ▼
  *    Initial
  *      |
  *      | (inferrer)
  *      ▼
  *  Augmented
  *       |
  *       | (ingestor)
  *       ▼
  *    Indexed
  *
  */
object ImageState {

  case class Initial(
    sourceIdentifier: SourceIdentifier,
    canonicalId: CanonicalId
  ) extends ImageState {
    type TransitionArgs = Unit
  }

  case class Augmented(
    sourceIdentifier: SourceIdentifier,
    canonicalId: CanonicalId,
    inferredData: Option[InferredData] = None
  ) extends ImageState {
    type TransitionArgs = Option[InferredData]
  }

  case class Indexed(
    sourceIdentifier: SourceIdentifier,
    canonicalId: CanonicalId,
    inferredData: Option[InferredData] = None,
    derivedData: DerivedImageData
  ) extends ImageState {
    type TransitionArgs = Unit
  }
}

// ImageFsm contains all of the possible transitions between image states
object ImageFsm {
  import ImageState._

  sealed trait Transition[InState <: ImageState, OutState <: ImageState] {
    def state(self: Image[InState], args: OutState#TransitionArgs): OutState
  }

  implicit val initialToAugmented = new Transition[Initial, Augmented] {
    def state(
      self: Image[Initial],
      inferredData: Option[InferredData]
    ): Augmented =
      Augmented(
        sourceIdentifier = self.state.sourceIdentifier,
        canonicalId = self.state.canonicalId,
        inferredData = inferredData
      )
  }

  implicit val augmentedToIndexed = new Transition[Augmented, Indexed] {
    def state(self: Image[Augmented], args: Unit): Indexed =
      Indexed(
        sourceIdentifier = self.state.sourceIdentifier,
        canonicalId = self.state.canonicalId,
        inferredData = self.state.inferredData,
        derivedData = DerivedImageData(self)
      )
  }
}

case class InferredData(
  // We split the feature vector so that it can fit into
  // ES's dense vector type (max length 2048)
  features1: List[Float],
  features2: List[Float],
  lshEncodedFeatures: List[String],
  palette: List[String],
  binSizes: List[List[Int]],
  binMinima: List[Float],
  aspectRatio: Option[Float]
)

object InferredData {
  def empty: InferredData = InferredData(Nil, Nil, Nil, Nil, Nil, Nil, None)
}
