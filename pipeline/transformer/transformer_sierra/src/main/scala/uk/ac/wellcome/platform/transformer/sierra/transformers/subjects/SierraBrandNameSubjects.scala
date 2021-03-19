package uk.ac.wellcome.platform.transformer.sierra.transformers.subjects

import uk.ac.wellcome.platform.transformer.sierra.source.VarField
import uk.ac.wellcome.platform.transformer.sierra.transformers.SierraAgents
import weco.catalogue.internal_model.work.{Concept, Subject}
import weco.catalogue.source_model.sierra.SierraBibNumber

// Populate wwork:subject
//
// Use MARC field "652". This is not documented but is a custom field used to
// represent brand names
object SierraBrandNameSubjects
    extends SierraSubjectsTransformer
    with SierraAgents {

  val subjectVarFields = List("652")

  def getSubjectsFromVarFields(bibId: SierraBibNumber,
                               varFields: List[VarField]) =
    varFields
      .subfieldsWithTag("a")
      .contents
      .map(label => Subject(label, List(Concept(label))))
}
