package weco.catalogue.sierra_adapter.linker

import uk.ac.wellcome.sierra_adapter.model.AbstractSierraRecord

trait SierraLinker[Record <: AbstractSierraRecord[_], Link] {
  def mergeLinks(existingLink: Link, updatedRecord: Record): Option[Link]
}
