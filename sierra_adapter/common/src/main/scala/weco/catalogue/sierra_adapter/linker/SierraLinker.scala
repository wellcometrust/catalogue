package weco.catalogue.sierra_adapter.linker

import uk.ac.wellcome.sierra_adapter.model.AbstractSierraRecord

trait SierraLinker[Record <: AbstractSierraRecord[_], Link <: SierraLink] {
  def createNewLink(record: Record): Link

  def mergeLinks(existingLink: Link, updatedRecord: Record): Option[Link]

  def updateRecord(record: Record, updatedLink: Link): Record
}
