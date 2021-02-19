package uk.ac.wellcome.platform.sierra_items_to_dynamo.models

import uk.ac.wellcome.sierra_adapter.model.{SierraBibNumber, SierraItemRecord}
import weco.catalogue.sierra_adapter.linker.SierraLink

import java.time.Instant

case class SierraItemLink(
  bibIds: List[SierraBibNumber],
  unlinkedBibIds: List[SierraBibNumber],
  modifiedDate: Instant
) extends SierraLink

case object SierraItemLink {
  def apply(record: SierraItemRecord): SierraItemLink =
    SierraItemLink(
      bibIds = record.bibIds,
      unlinkedBibIds = record.unlinkedBibIds,
      modifiedDate = record.modifiedDate
    )
}
