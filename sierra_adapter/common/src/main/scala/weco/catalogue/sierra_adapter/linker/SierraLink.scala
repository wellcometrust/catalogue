package weco.catalogue.sierra_adapter.linker

import uk.ac.wellcome.sierra_adapter.model.SierraBibNumber

import java.time.Instant

trait SierraLink {
  val bibIds: List[SierraBibNumber]
  val unlinkedBibIds: List[SierraBibNumber]
  val modifiedDate: Instant
}
