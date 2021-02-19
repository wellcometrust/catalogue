package weco.catalogue.sierra_item_linker.fixtures

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.sierra_adapter.model.{SierraItemNumber, SierraItemRecord}
import uk.ac.wellcome.storage.store.memory.MemoryVersionedStore
import weco.catalogue.sierra_adapter.linker.{
  LinkerWorkerServiceFixture,
  SierraLinkStore,
  SierraLinker
}
import weco.catalogue.sierra_item_linker.linker.{
  SierraItemLink,
  SierraItemLinkStore,
  SierraItemLinker
}

trait ItemLinkerWorkerServiceFixture extends LinkerWorkerServiceFixture[SierraItemNumber, SierraItemRecord, SierraItemLink] {
  val linker: SierraLinker[SierraItemRecord, SierraItemLink] =
    SierraItemLinker

  def withLinkStore[R](store: MemoryVersionedStore[SierraItemNumber, SierraItemLink])(testWith: TestWith[SierraLinkStore[SierraItemNumber, SierraItemRecord, SierraItemLink], R]): R =
    testWith(
      new SierraItemLinkStore(store)
    )
}
