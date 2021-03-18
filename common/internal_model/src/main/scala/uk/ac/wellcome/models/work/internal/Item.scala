package uk.ac.wellcome.models.work.internal

import weco.catalogue.internal_model.identifiers.{HasId, IdState}
import weco.catalogue.internal_model.locations.Location

case class Item[+State](
  id: State,
  title: Option[String] = None,
  locations: List[Location] = Nil
) extends HasId[State]

object Item {

  def apply[State >: IdState.Unidentifiable.type](
    title: Option[String],
    locations: List[Location]
  ): Item[State] =
    Item(IdState.Unidentifiable, title, locations)
}
