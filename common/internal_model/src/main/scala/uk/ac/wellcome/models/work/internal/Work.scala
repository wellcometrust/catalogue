package uk.ac.wellcome.models.work.internal

/** Work contains the work itself. It is parameterised by its state, meaning
  * the same type of Work can be in a number of possible states depending on
  * where in the pipeline it is. This allows us to easily add new types of work
  * (such as if Collection is decided to be a separate type to StandardWork),
  * with the state of the work in the pipeline being an orthogonal concern.
  */
sealed trait Work[State <: WorkState, ImageId <: IdState.WithSourceIdentifier] {
  val sourceIdentifier: SourceIdentifier
  val version: Int
  def maybeData: Option[WorkData[State#DataId, ImageId]]
}

object Work {
  case class Standard[State <: WorkState, ImageId <: IdState.WithSourceIdentifier](
    sourceIdentifier: SourceIdentifier,
    version: Int,
    data: WorkData[State#DataId, ImageId],
    state: State,
  ) extends Work[State, ImageId] {

    def maybeData = Some(data)
  }

  case class Redirected[State <: WorkState, ImageId <: IdState.WithSourceIdentifier](
    sourceIdentifier: SourceIdentifier,
    version: Int,
    state: State,
  ) extends Work[State, ImageId] {

    def maybeData = None
  }

  case class Invisible[State <: WorkState, ImageId <: IdState.WithSourceIdentifier](
    sourceIdentifier: SourceIdentifier,
    version: Int,
    data: WorkData[State#DataId, ImageId],
    state: State,
  ) extends Work[State, ImageId] {

    def maybeData = Some(data)
  }
}

/** WorkData contains data common to all types of works that can exist at any
  * stage of the pipeline.
  */
case class WorkData[DataId <: IdState, ImageId <: IdState.WithSourceIdentifier](
  title: Option[String] = None,
  otherIdentifiers: List[SourceIdentifier] = Nil,
  mergeCandidates: List[MergeCandidate] = Nil,
  alternativeTitles: List[String] = Nil,
  workType: Option[WorkType] = None,
  description: Option[String] = None,
  physicalDescription: Option[String] = None,
  lettering: Option[String] = None,
  createdDate: Option[Period[DataId]] = None,
  subjects: List[Subject[DataId]] = Nil,
  genres: List[Genre[DataId]] = Nil,
  contributors: List[Contributor[DataId]] = Nil,
  thumbnail: Option[LocationDeprecated] = None,
  production: List[ProductionEvent[DataId]] = Nil,
  language: Option[Language] = None,
  edition: Option[String] = None,
  notes: List[Note] = Nil,
  duration: Option[Int] = None,
  items: List[Item[DataId]] = Nil,
  merged: Boolean = false,
  collectionPath: Option[CollectionPath] = None,
  images: List[UnmergedImage[ImageId, DataId]] = Nil
)

/** WorkState represents the state of the work in the pipeline, and contains
  * different data depending on what state it is. This allows us to consider the
  * Work model as a finite state machine with the following stages corresponding
  * to stages of the pipeline:
  *
  *      |
  *      | (transformer)
  *      ▼
  *   Unmerged
  *      |
  *      | (matcher / merger)
  *      ▼
  *   Merged
  *      |
  *      | (relation embedder)
  *      ▼
  * Denormalised
  *      |
  *      | (id minter)
  *      ▼
  *  Identified
  *
  * Each WorkState also has an associated IdentifierState which indicates whether
  * the corresponding WorkData is pre or post the minter.
  */
sealed trait WorkState {

  type DataId <: IdState

  val sourceIdentifier: SourceIdentifier
}

object WorkState {

  // TODO: for now just 2 states, in the end the states will correspond to the
  // block comment above

  case class Unidentified(
    sourceIdentifier: SourceIdentifier,
    identifiedType: String = classOf[Identified].getSimpleName,
  ) extends WorkState {

    type DataId = IdState.Unminted
  }

  case class Identified(
    sourceIdentifier: SourceIdentifier,
    canonicalId: String,
    // relations: Relations[Identified],
  ) extends WorkState {

    type DataId = IdState.Minted
  }
}
