package uk.ac.wellcome.models.work.internal

case class DerivedWorkData(
  availableOnline: Boolean,
  contributorAgents: List[String] = Nil,
)

object DerivedWorkData {

  def none: DerivedWorkData =
    DerivedWorkData(
      availableOnline = false,
      contributorAgents = Nil
    )

  def apply(data: WorkData[_]): DerivedWorkData =
    DerivedWorkData(
      availableOnline =
        containsLocation(_.isInstanceOf[DigitalLocation])(data.items),
      contributorAgents = contributorAgents(data.contributors)
    )

  private def contributorAgents(
    contributors: List[Contributor[_]]): List[String] =
    contributors.map {
      case Contributor(_, agent, _) =>
        val ontologyType = agent.getClass.getSimpleName
        val label = agent.label
        s"$ontologyType:$label"
    }

  private def containsLocation(predicate: Location => Boolean)(
    items: List[Item[_]]): Boolean =
    items.exists { item =>
      item.locations.exists(predicate)
    }
}
