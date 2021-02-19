package weco.catalogue.sierra_item_linker.fixtures

import uk.ac.wellcome.sierra_adapter.model.{SierraItemNumber, SierraItemRecord}
import weco.catalogue.sierra_adapter.linker.{
  LinkerWorkerServiceFixture,
  SierraLinker
}
import weco.catalogue.sierra_item_linker.linker.{
  SierraItemLink,
  SierraItemLinker
}

trait ItemLinkerWorkerServiceFixture
    extends LinkerWorkerServiceFixture[
      SierraItemNumber,
      SierraItemRecord,
      SierraItemLink] {
  val linker: SierraLinker[SierraItemRecord, SierraItemLink] =
    SierraItemLinker
}
