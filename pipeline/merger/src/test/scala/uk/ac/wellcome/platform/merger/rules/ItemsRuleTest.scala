package uk.ac.wellcome.platform.merger.rules

import org.scalatest.Inside
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.internal.Format
import uk.ac.wellcome.platform.merger.generators.WorksWithImagesGenerators
import uk.ac.wellcome.platform.merger.models.FieldMergeResult

class ItemsRuleTest
    extends AnyFunSpec
    with Matchers
    with WorksWithImagesGenerators
    with Inside {
  val physicalPictureSierra = createSierraPhysicalWork.copy(
    data = createSierraPhysicalWork.data.copy(
      format = Some(Format.Pictures)
    )
  )
  val physicalMapsSierra = physicalPictureSierra.copy(
    data = physicalPictureSierra.data.copy(
      format = Some(Format.Maps)
    )
  )
  val zeroItemPhysicalSierra = createUnidentifiedSierraWork.copy(
    data = createUnidentifiedSierraWork.data.copy(
      format = Some(Format.Pictures)
    )
  )
  val multiItemPhysicalSierra = createSierraWorkWithTwoPhysicalItems
  val digitalSierra = createSierraDigitalWork
  val metsWork = createUnidentifiedInvisibleMetsWork
  val miroWork = createMiroWork
  val calmWork = createUnidentifiedCalmWork
  val (sierraWorkWithMergeCandidate, sierraWorkMergeCandidate) =
    createSierraWorkWithDigitisedMergeCandidate

  it(
    "leaves items unchanged and returns a digitised version of a Sierra work as a merged source") {
    inside(
      ItemsRule
        .merge(sierraWorkWithMergeCandidate, List(sierraWorkMergeCandidate))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items should be(sierraWorkWithMergeCandidate.data.items)

        mergedSources should be(Seq(sierraWorkMergeCandidate))
    }
  }

  // Sierra zero item
  it("merges the item from Miro works into zero-item Sierra works") {
    inside(ItemsRule.merge(zeroItemPhysicalSierra, List(miroWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head shouldBe miroWork.data.items.head
        mergedSources should be(Seq(miroWork))
    }
  }

  it("merges the item from METS works into zero-item Sierra works") {
    inside(ItemsRule.merge(zeroItemPhysicalSierra, List(metsWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head shouldBe metsWork.data.items.head
        mergedSources should be(Seq(metsWork))
    }
  }

  // Sierra single item
  it("merges locations from Miro items into single-item Sierra works") {
    inside(ItemsRule.merge(physicalPictureSierra, List(miroWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head.locations should contain theSameElementsAs
          physicalPictureSierra.data.items.head.locations ++ miroWork.data.items.head.locations

        mergedSources should be(Seq(miroWork))
    }
  }

  it("merges item locations in METS work into single-item Sierra works item") {
    inside(ItemsRule.merge(physicalPictureSierra, List(metsWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head.locations shouldBe
          physicalPictureSierra.data.items.head.locations ++
            metsWork.data.items.head.locations

        mergedSources should be(Seq(metsWork))
    }
  }

  it("does not merge any Miro sources when there are several of them") {
    inside(
      ItemsRule.merge(physicalPictureSierra, List(miroWork, createMiroWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items shouldEqual physicalPictureSierra.data.items
        mergedSources shouldBe empty
    }
  }

  it(
    "does not merge a Miro source into a Sierra work with format != picture/digital image/3D object") {
    inside(ItemsRule.merge(physicalMapsSierra, List(miroWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items shouldEqual physicalMapsSierra.data.items
        mergedSources shouldBe empty
    }
  }

  it(
    "override Miro merging with METS merging into single-item physical Sierra works items") {
    inside(ItemsRule.merge(physicalPictureSierra, List(miroWork, metsWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head.locations shouldBe
          physicalPictureSierra.data.items.head.locations ++
            metsWork.data.items.head.locations

        mergedSources should contain theSameElementsAs (Seq(metsWork, miroWork))
    }
  }

  // Sierra multi items
  it("doesn't merge Miro works into multi-item Sierra works") {
    inside(ItemsRule.merge(multiItemPhysicalSierra, List(miroWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should be(multiItemPhysicalSierra.data.items)
        mergedSources should be(Seq())
    }
  }

  it("adds items from METS works into multi-item Sierra works") {
    inside(ItemsRule.merge(multiItemPhysicalSierra, List(metsWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should contain theSameElementsAs
          multiItemPhysicalSierra.data.items ++ metsWork.data.items
        mergedSources should be(Seq(metsWork))
    }
  }

  // Calm
  it("Adds Sierra item IDs to Calm item") {
    inside(ItemsRule.merge(calmWork, List(physicalPictureSierra))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head.id should be(physicalPictureSierra.data.items.head.id)

        mergedSources should be(Seq(physicalPictureSierra))
    }
  }

  it("Adds the METS item location to the Calm item") {
    inside(ItemsRule.merge(calmWork, List(metsWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head.locations should contain theSameElementsAs (calmWork.data.items.head.locations ++ metsWork.data.items.head.locations)

        mergedSources should be(Seq(metsWork))
    }
  }

  it("Adds Sierra item IDs and the METS item location to a Calm work") {
    inside(ItemsRule.merge(calmWork, List(physicalPictureSierra, metsWork))) {
      case FieldMergeResult(items, mergedSources) =>
        items should have size 1
        items.head.id should be(physicalPictureSierra.data.items.head.id)
        items.head.locations should contain theSameElementsAs (calmWork.data.items.head.locations ++ metsWork.data.items.head.locations)

        mergedSources should contain theSameElementsAs Seq(
          metsWork,
          physicalPictureSierra)
    }
  }
}
