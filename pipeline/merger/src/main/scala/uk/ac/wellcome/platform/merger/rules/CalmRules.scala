package uk.ac.wellcome.platform.merger.rules

import cats.data.NonEmptyList

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.models.FieldMergeResult
import uk.ac.wellcome.platform.merger.logging.MergerLogging
import uk.ac.wellcome.platform.merger.rules.WorkPredicates.WorkPredicate

trait UseCalmWhenExistsRule extends FieldMergeRule with MergerLogging {

  protected val getData: TransformedBaseWork => FieldData

  override def merge(
    target: UnidentifiedWork,
    sources: Seq[TransformedBaseWork]): FieldMergeResult[FieldData] =
    FieldMergeResult(
      fieldData = calmRule(target, sources) getOrElse getData(target),
      redirects = Nil
    )

  val calmRule =
    new PartialRule {
      val isDefinedForTarget: WorkPredicate = WorkPredicates.sierraWork
      val isDefinedForSource: WorkPredicate = WorkPredicates.calmWork

      def rule(sierraWork: UnidentifiedWork,
               calmWorks: NonEmptyList[TransformedBaseWork]): FieldData =
        getData(calmWorks.head)
    }
}

object UseCalmWhenExistsRule {
  def apply[T](f: TransformedBaseWork => T) =
    new UseCalmWhenExistsRule {
      type FieldData = T
      protected val getData = f
    }
}

object CalmRules {

  val CollectionRule = UseCalmWhenExistsRule(_.data.collection)

  val TitleRule = UseCalmWhenExistsRule(_.data.title)

  val PhysicalDescriptionRule = UseCalmWhenExistsRule(
    _.data.physicalDescription)

  val ContributorsRule = UseCalmWhenExistsRule(_.data.contributors)

  val SubjectsRule = UseCalmWhenExistsRule(_.data.subjects)

  val LanguageRule = UseCalmWhenExistsRule(_.data.language)

  val NotesRule = UseCalmWhenExistsRule(_.data.notes)

  val WorkTypeRule = UseCalmWhenExistsRule(_.data.workType)
}
