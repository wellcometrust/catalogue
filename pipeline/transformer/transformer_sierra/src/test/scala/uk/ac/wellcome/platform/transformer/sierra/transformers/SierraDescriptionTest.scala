package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraDescriptionTest
    extends AnyFunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  it(
    "extracts a work description where MARC field 520 with subfield a is populated") {
    val description = "A panolopy of penguins perching on a python."
    val expectedDescription = description

    assertFindsCorrectDescription(
      varFields = List(
        createVarFieldWith(
          marcTag = "520",
          subfields = List(
            MarcSubfield(tag = "a", content = description)
          )
        )
      ),
      expectedDescription = Some(expectedDescription)
    )
  }

  it("extracts a work description where there are multiple MARC field 520") {
    val description1 = "A malcontent marc minion."
    val description2 = "A fresh fishy fruit."
    val summaryDescription2 = "A case of colloidal coffee capsules."

    val expectedDescription = s"$description1 $description2 $summaryDescription2"

    assertFindsCorrectDescription(
      varFields = List(
        createVarFieldWith(
          marcTag = "520",
          subfields = List(
            MarcSubfield(tag = "a", content = description1)
          )
        ),
        createVarFieldWith(
          marcTag = "520",
          subfields = List(
            MarcSubfield(tag = "a", content = description2),
            MarcSubfield(tag = "b", content = summaryDescription2)
          )
        )
      ),
      expectedDescription = Some(expectedDescription)
    )
  }

  it(
    "extracts a work description where MARC field 520 with subfield a and b are populated") {
    val description = "A panolopy of penguins perching on a python."
    val summaryDescription = "A fracas of frolicking frogs on futons."

    val expectedDescription = s"$description $summaryDescription"

    assertFindsCorrectDescription(
      varFields = List(
        createVarFieldWith(
          marcTag = "520",
          subfields = List(
            MarcSubfield(tag = "a", content = description),
            MarcSubfield(tag = "b", content = summaryDescription)
          )
        )
      ),
      expectedDescription = Some(expectedDescription)
    )
  }

  it("does not extract a work description where MARC field 520 is absent") {
    assertFindsCorrectDescription(
      varFields = List(
        createVarFieldWith(marcTag = "666")
      ),
      expectedDescription = None
    )
  }

  private def assertFindsCorrectDescription(
    varFields: List[VarField],
    expectedDescription: Option[String]
  ): Assertion = {
    val actualDescription = SierraDescription(
      createSierraBibDataWith(varFields = varFields))
    actualDescription shouldBe expectedDescription
  }
}
