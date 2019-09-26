package uk.ac.wellcome.platform.transformer.sierra.source


trait SierraQueryOps {

  implicit class BibDataOps(bibData: SierraBibData) {

    def varfields: List[VarField] = bibData.varFields

    def varfieldsWithTags(tags: String*): List[VarField] =
      bibData.varFields.withTags(tags:_*)

    def varfieldsWithTag(tag: String): List[VarField] =
      varfieldsWithTags(tag)

    def subfieldsWithTags(tags: (String, String)*): List[MarcSubfield] =
      tags.toList.flatMap { case (tag, subfieldTag) =>
        varfieldsWithTag(tag).subfieldsWithTag(subfieldTag)
      }

    def subfieldsWithTag(tag: (String, String)): List[MarcSubfield] =
      subfieldsWithTags(tag)
  }

  implicit class VarFieldsOps(varfields: List[VarField]) {

    def withTags(tags: String*): List[VarField] =
      varfields
        .filter { _.marcTag.map(tag => tags.contains(tag)).getOrElse(false) }
        .sortBy { varfield => tags.indexOf(varfield.marcTag.get) }

    def withTags(tag: String): List[VarField] = withTags(tag)

    def withIndicator1(ind: String): List[VarField] =
      varfields.filter(_.indicator1 == Some(ind))

    def withIndicator2(ind: String): List[VarField] =
      varfields.filter(_.indicator2 == Some(ind))

    def subfields: List[MarcSubfield] = varfields.flatMap(_.subfields)

    def subfieldsWithTags(tags: String*): List[MarcSubfield] =
      varfields.subfields.withTags(tags:_*)

    def subfieldsWithTag(tag: String): List[MarcSubfield] =
      subfieldsWithTags(tag)

    def contents: List[String] = varfields.flatMap(_.content)

    def subfieldContents: List[String] = varfields.subfields.contents

    def firstContent: Option[String] = varfields.contents.headOption

    def contentString(sep: String): Option[String] = contents.mkStringOrNone(sep)

    def contentString: Option[String] = contents.mkStringOrNone
  }

  implicit class SubfieldsOps(subfields: List[MarcSubfield]) {

    def withTags(tags: String*): List[MarcSubfield] =
      subfields
        .filter { subfield => tags.contains(subfield.tag) }

    def withTag(tag: String): List[MarcSubfield] = withTags(tag)

    def contents: List[String] = subfields.map(_.content)

    def firstContent: Option[String] = subfields.contents.headOption

    def contentString(sep: String): Option[String] = contents.mkStringOrNone(sep)

    def contentString: Option[String] = contents.mkStringOrNone
  }

  implicit class StringSeqOps(strings: Seq[String]) {

    def mkStringOrNone(sep: String): Option[String] =
      strings match {
        case Nil => None
        case strings => Some(strings.mkString(sep))
      }

    def mkStringOrNone: Option[String] =
      strings match {
        case Nil => None
        case strings => Some(strings.mkString)
      }
  }

  import scala.language.implicitConversions

  implicit def varfieldOps(varfield: VarField): VarFieldsOps =
    new VarFieldsOps(List(varfield))
}
