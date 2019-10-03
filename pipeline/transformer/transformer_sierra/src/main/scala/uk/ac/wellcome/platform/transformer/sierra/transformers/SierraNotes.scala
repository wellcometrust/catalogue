package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.transformable.sierra.SierraBibNumber
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  SierraQueryOps
}

object SierraNotes extends SierraTransformer with SierraQueryOps {

  type Output = List[Note]

  val notesFields: List[(String, String => Note)] =
    List(
      "500" -> GeneralNote.apply,
      "501" -> GeneralNote.apply,
      "504" -> BibliographicalInformation.apply,
      "518" -> TimeAndPlaceNote.apply,
      "536" -> FundingInformation.apply,
      "545" -> BibliographicalInformation.apply,
      "547" -> GeneralNote.apply,
      "562" -> GeneralNote.apply
    )

  def apply(bibId: SierraBibNumber, bibData: SierraBibData) =
    notesFields
      .flatMap {
        case (tag, createNote) =>
          bibData
            .varfieldsWithTags(tag)
            .subfieldContents
            .map(createNote)
      }
}
