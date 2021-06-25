package weco.pipeline.transformer.sierra.transformers

import grizzled.slf4j.Logging
import weco.catalogue.internal_model.languages.{Language, MarcLanguageCodeList}
import weco.catalogue.source_model.sierra.source.{
  SierraQueryOps,
  SierraSourceLanguage
}
import weco.catalogue.source_model.sierra.SierraBibData
import weco.catalogue.source_model.sierra.identifiers.SierraBibNumber

object SierraLanguages
    extends SierraIdentifiedDataTransformer
    with SierraQueryOps
    with Logging {
  type Output = List[Language]

  // We only want to display languages that actually correspond to languages.
  // These language codes don't tell us anything useful, so we suppress them.
  private val suppressedLanguageCodes: Set[String] = Set(
    "mul",  // Multiple languages
    "und",  // Undetermined
    "zxx",  // No linguistic content
  )

  // Populate wwork:language.
  //
  // Rules:
  //
  //  - The first language comes from 008/35-37.  The Sierra API provides
  //    this via the "lang" field.
  //    See https://www.loc.gov/marc/bibliographic/bd008.html
  //
  //  - Subsequent languages come from MARC 041 ǂa.
  //    This is a repeatable field, as is the subfield.
  //    See https://www.loc.gov/marc/bibliographic/bd041.html
  //
  override def apply(bibId: SierraBibNumber, bibData: SierraBibData): List[Language] = {
    val primaryLanguage = bibData.lang.flatMap { createLanguage(bibId, _) }

    val additionalLanguages =
      bibData
        .subfieldsWithTag("041" -> "a")
        .contents
        .map {
          // Some of our records include whitespace in this field, but it's irrelevant
          // and can be safely discarded.  e.g. "ger " means the same as "ger"
          _.trim
        }
        .map {
          // Most of our records use a lowercase code, but if not we can safely lowercase
          // the code.  e.g. "Lat" means the same as "lat"
          _.toLowerCase()
        }
        .map { code =>
          (code, MarcLanguageCodeList.fromCode(code))
        }
        .map {
          case (_, Some(lang)) => Some(lang)
          case (code, None) =>
            warn(s"$bibId: Unrecognised code in MARC 041 ǂa: $code")
            None
        }

    (List(primaryLanguage) ++ additionalLanguages)
      .flatten
      .filterNot { lang => suppressedLanguageCodes.contains(lang.id) }
      .distinct
  }

  private def createLanguage(bibId: SierraBibNumber, lang: SierraSourceLanguage): Option[Language] =
    (lang, MarcLanguageCodeList.fromCode(lang.code)) match {
      case (_, Some(deducedLang)) => Some(deducedLang)
      case (SierraSourceLanguage(code, Some(name)), _) => Some(Language(label = name, id = code))
      case _ =>
        warn(s"$bibId: Unrecognised primary language: $lang")
        None
    }
}