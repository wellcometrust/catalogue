package weco.catalogue.sierra_item_linker.linker

import uk.ac.wellcome.platform.sierra_items_to_dynamo.models.SierraItemLink
import uk.ac.wellcome.sierra_adapter.model.{SierraItemNumber, SierraItemRecord}
import uk.ac.wellcome.storage.store.VersionedStore
import weco.catalogue.sierra_adapter.linker.{SierraLinkStore, SierraLinker}

class SierraItemLinkStore(
  val store: VersionedStore[SierraItemNumber, Int, SierraItemLink]
) extends SierraLinkStore[SierraItemNumber, SierraItemRecord, SierraItemLink] {
  override val linker: SierraLinker[SierraItemRecord, SierraItemLink] = SierraItemLinker
}
