package weco.pipeline.merger.rules

import org.scalatest.Inside
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus,
  DigitalLocation,
  License,
  LocationType
}
import weco.catalogue.internal_model.work.generators.SourceWorkGenerators
import weco.pipeline.merger.models.FieldMergeResult

class ThumbnailRuleTest
    extends AnyFunSpec
    with Matchers
    with SourceWorkGenerators
    with Inside {

  val physicalSierraWork = sierraPhysicalIdentifiedWork()

  val digitalSierraWork = sierraDigitalIdentifiedWork()

  val calmWork = calmIdentifiedWork()
  val teiWork = teiIdentifiedWork()

  val metsWork = metsIdentifiedWork()
    .thumbnail(
      DigitalLocation(
        url = "mets.com/thumbnail.jpg",
        locationType = LocationType.ThumbnailImage,
        license = Some(License.CCBY)
      )
    )

  val miroWorks = (0 to 3).map(_ => miroIdentifiedWork())

  val restrictedDigitalWork =
    sierraIdentifiedWork().items(
      List(
        createUnidentifiableItemWith(
          locations = List(
            createDigitalLocationWith(
              accessConditions = List(
                AccessCondition(
                  method = AccessMethod.ViewOnline,
                  status = AccessStatus.Restricted
                )
              )
            )
          )
        )
      )
    )

  it(
    "chooses the METS thumbnail from a single-item digital METS work for a digital Sierra target") {
    inside(ThumbnailRule.merge(digitalSierraWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        thumbnail shouldBe metsWork.data.thumbnail
    }
  }

  it("chooses the METS thumbnail for a Calm target") {
    inside(ThumbnailRule.merge(calmWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        thumbnail shouldBe metsWork.data.thumbnail
    }
  }

  it("chooses the METS thumbnail for a Tei target") {
    inside(ThumbnailRule.merge(teiWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        thumbnail shouldBe metsWork.data.thumbnail
    }
  }

  it(
    "chooses a Miro thumbnail if no METS works are available, for a Tei target") {
    inside(ThumbnailRule.merge(teiWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        miroWorks.map(_.data.thumbnail) should contain(thumbnail)
    }
  }

  it(
    "chooses a Miro thumbnail if no METS works are available, for a digital Sierra target") {
    inside(ThumbnailRule.merge(digitalSierraWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        miroWorks.map(_.data.thumbnail) should contain(thumbnail)
    }
  }

  it(
    "chooses a Miro thumbnail if no METS works are available, for a physical Sierra target") {
    inside(ThumbnailRule.merge(physicalSierraWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        miroWorks.map(_.data.thumbnail) should contain(thumbnail)
    }
  }

  it("chooses the Miro thumbnail from the work with the minimum ID") {
    inside(ThumbnailRule.merge(physicalSierraWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        thumbnail shouldBe miroWorks
          .minBy(_.sourceIdentifier.value)
          .data
          .thumbnail
    }
  }

  it("suppresses thumbnails when restricted access status") {
    inside(ThumbnailRule.merge(restrictedDigitalWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe None
    }
  }

  it("returns redirects of merged thumbnails") {
    inside(ThumbnailRule.merge(digitalSierraWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(_, redirects) =>
        redirects should contain theSameElementsAs miroWorks :+ metsWork
    }
  }
}
