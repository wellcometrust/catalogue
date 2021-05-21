package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators
import uk.ac.wellcome.platform.transformer.sierra.source.FixedField

class SierraRulesForRequestingTest extends AnyFunSpec with Matchers with SierraDataGenerators with TableDrivenPropertyChecks {
  it("blocks an item from the strong room") {
    val item = createSierraItemDataWith(
      fixedFields = Map("97" -> FixedField(label = "IMESSAGE", value = "x"))
    )

    SierraRulesForRequesting(item) shouldBe NotRequestable("This item belongs in the Strongroom")
  }

  it("blocks an item based on the status") {
    val testCases = Table(
      ("status", "expectedMessage"),
      ("m", Some("This item is missing.")),
      ("s", Some("This item is on search.")),
      ("x", Some("This item is withdrawn.")),
      ("r", Some("This item is unavailable.")),
      ("z", None),
      ("v", Some("This item is with conservation.")),
      ("h", Some("This item is closed.")),
      ("b", Some("Please request top item.")),
      ("c", Some("Please request top item.")),
      ("d", Some("On new books display.")),
      ("e", Some("On exhibition. Please ask at Enquiry Desk.")),
      ("y", None),

    )

    forAll(testCases) { case (status, expectedMessage) =>
      val item = createSierraItemDataWith(
        fixedFields = Map("88" -> FixedField(label = "STATUS", value = status))
      )

      SierraRulesForRequesting(item) shouldBe NotRequestable(expectedMessage)
    }
  }

  it("allows an item that does not match any rules") {
    val item = createSierraItemData

    SierraRulesForRequesting(item) shouldBe Requestable
  }
}
