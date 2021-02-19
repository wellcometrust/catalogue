package weco.catalogue.sierra_item_linker.linker

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.platform.sierra_items_to_dynamo.models.SierraItemLink
import uk.ac.wellcome.sierra_adapter.model.SierraGenerators

class SierraItemLinkerTest extends AnyFunSpec with Matchers with SierraGenerators {
  describe("createNewLink") {
    it("copies the data from the item record") {
      val bibIds = createSierraBibNumbers(count = 5)
      val unlinkedBibIds = createSierraBibNumbers(count = 3)
      val modifiedDate = randomInstant

      val record = createSierraItemRecordWith(
        bibIds = bibIds,
        unlinkedBibIds = unlinkedBibIds,
        modifiedDate = modifiedDate
      )

      val expectedLink = SierraItemLink(
        bibIds = bibIds,
        unlinkedBibIds = unlinkedBibIds,
        modifiedDate = modifiedDate
      )

      SierraItemLinker.createNewLink(record) shouldBe expectedLink
    }
  }

  describe("mergeLinks") {
    it("combines the bibIds in the final result") {
      val bibIds = createSierraBibNumbers(count = 5)
      val existingRecord = createSierraItemRecordWith(
        modifiedDate = olderDate,
        bibIds = bibIds.slice(0, 3)
      )
      val newRecord = createSierraItemRecordWith(
        id = existingRecord.id,
        modifiedDate = newerDate,
        bibIds = bibIds
      )

      val mergedRecord =
        SierraItemLinker
          .mergeLinks(
            existingLink = SierraItemLink(existingRecord),
            newRecord = newRecord)
          .get
      mergedRecord.bibIds shouldBe bibIds
      mergedRecord.unlinkedBibIds shouldBe List()
    }

    it("records unlinked bibIds") {
      val bibIds = createSierraBibNumbers(count = 5)

      val existingRecord = createSierraItemRecordWith(
        modifiedDate = olderDate,
        bibIds = bibIds.slice(0, 3)
      )
      val newRecord = createSierraItemRecordWith(
        id = existingRecord.id,
        modifiedDate = newerDate,
        bibIds = bibIds.slice(2, 5)
      )

      val mergedRecord =
        SierraItemLinker
          .mergeLinks(
            existingLink = SierraItemLink(existingRecord),
            newRecord = newRecord)
          .get
      mergedRecord.bibIds shouldBe bibIds.slice(2, 5)
      mergedRecord.unlinkedBibIds shouldBe bibIds.slice(0, 2)
    }

    it("preserves existing unlinked bibIds") {
      val bibIds = createSierraBibNumbers(count = 5)

      val existingRecord = createSierraItemRecordWith(
        modifiedDate = olderDate,
        bibIds = bibIds.slice(0, 3),
        unlinkedBibIds = bibIds.slice(3, 5)
      )
      val newRecord = createSierraItemRecordWith(
        id = existingRecord.id,
        modifiedDate = newerDate,
        bibIds = existingRecord.bibIds
      )

      val mergedRecord =
        SierraItemLinker
          .mergeLinks(
            existingLink = SierraItemLink(existingRecord),
            newRecord = newRecord)
          .get
      mergedRecord.unlinkedBibIds should contain theSameElementsAs existingRecord.unlinkedBibIds
    }

    it("does not duplicate unlinked bibIds") {
      // This would be an unusual scenario to arise, but check we handle it anyway!
      val bibIds = createSierraBibNumbers(count = 3)

      val existingRecord = createSierraItemRecordWith(
        modifiedDate = olderDate,
        bibIds = bibIds,
        unlinkedBibIds = List(bibIds(2))
      )
      val newRecord = createSierraItemRecordWith(
        id = existingRecord.id,
        modifiedDate = newerDate,
        bibIds = bibIds.slice(0, 2)
      )

      val mergedRecord =
        SierraItemLinker
          .mergeLinks(
            existingLink = SierraItemLink(existingRecord),
            newRecord = newRecord)
          .get
      mergedRecord.bibIds shouldBe bibIds.slice(0, 2)
      mergedRecord.unlinkedBibIds shouldBe List(bibIds(2))
    }

    it("removes an unlinked bibId if it appears on a new record") {
      val bibIds = createSierraBibNumbers(count = 3)

      val existingRecord = createSierraItemRecordWith(
        modifiedDate = olderDate,
        bibIds = List(),
        unlinkedBibIds = bibIds
      )
      val newRecord = createSierraItemRecordWith(
        id = existingRecord.id,
        modifiedDate = newerDate,
        bibIds = bibIds.slice(0, 2)
      )

      val mergedRecord =
        SierraItemLinker
          .mergeLinks(
            existingLink = SierraItemLink(existingRecord),
            newRecord = newRecord)
          .get
      mergedRecord.bibIds shouldBe bibIds.slice(0, 2)
      mergedRecord.unlinkedBibIds shouldBe List(bibIds(2))
    }

    it("returns None if it receives an outdated update") {
      val bibIds = createSierraBibNumbers(count = 5)

      val existingRecord = createSierraItemRecordWith(
        modifiedDate = newerDate,
        bibIds = bibIds.slice(0, 3)
      )
      val newRecord = createSierraItemRecordWith(
        id = existingRecord.id,
        modifiedDate = newerDate,
        bibIds = bibIds
      )

      val mergedRecord =
        SierraItemLinker.mergeLinks(
          existingLink = SierraItemLink(existingRecord),
          newRecord = newRecord)

      mergedRecord shouldBe None
    }
  }

  describe("updateRecord") {
    it("copies the unlinked IDs from the link") {
      val oldUnlinkedBibIds = createSierraBibNumbers(count = 3)
      val newUnlinkedBibIds = oldUnlinkedBibIds ++ createSierraBibNumbers(count = 2)

      val record = createSierraItemRecordWith(
        unlinkedBibIds = oldUnlinkedBibIds
      )

      val link = SierraItemLink(record).copy(
        unlinkedBibIds = newUnlinkedBibIds
      )

      SierraItemLinker.updateRecord(record, link) shouldBe record.copy(
        unlinkedBibIds = newUnlinkedBibIds
      )
    }
  }
}
