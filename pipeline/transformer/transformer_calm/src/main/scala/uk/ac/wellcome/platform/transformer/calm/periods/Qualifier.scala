package uk.ac.wellcome.platform.transformer.calm.periods

import fastparse._
import NoWhitespace._
import uk.ac.wellcome.models.parse._

sealed trait Qualifier
object Qualifier {
  case object Pre extends Qualifier
  case object Post extends Qualifier
  case object About extends Qualifier // Covers about, approx, circa
  case object Mid extends Qualifier
  case object Early extends Qualifier
  case object Late extends Qualifier
}

trait QualifyFuzzyDate[D <: FuzzyDate] {
  val toDateRange: PartialFunction[(Qualifier, D), FuzzyDateRange[Year, Year]]
}

object QualifyFuzzyDate extends ParserUtils {
  def qualified[_: P, D <: FuzzyDate](unqualified: => P[D])(
    implicit q: QualifyFuzzyDate[D]): P[FuzzyDateRange[Year, Year]] =
    (Lex.qualifier ~ ws.? ~ "-".? ~ ws.? ~ unqualified) collect q.toDateRange

  implicit val qualifyCentury =
    new QualifyFuzzyDate[Century] {
      lazy val toDateRange = {
        case (Qualifier.Early, Century(century)) =>
          FuzzyDateRange(
            Year(100 * century),
            Year(100 * century + 39),
          )
        case (Qualifier.Mid, Century(century)) =>
          FuzzyDateRange(
            Year(100 * century + 30),
            Year(100 * century + 69),
          )
        case (Qualifier.Late, Century(century)) =>
          FuzzyDateRange(
            Year(100 * century + 60),
            Year(100 * century + 99),
          )
      }
    }

  implicit val qualifyYear =
    new QualifyFuzzyDate[Year] {
      lazy val toDateRange = {
        case (Qualifier.About, Year(year)) =>
          FuzzyDateRange(
            Year(year - 10),
            Year(year + 9),
          )
        case (Qualifier.Pre, Year(year)) =>
          FuzzyDateRange(
            Year(year - 10),
            Year(year),
          )
        case (Qualifier.Post, Year(year)) =>
          FuzzyDateRange(
            Year(year),
            Year(year + 9),
          )
      }
    }

  implicit val qualifyDecade =
    new QualifyFuzzyDate[CenturyAndDecade] {
      lazy val toDateRange = {
        case (Qualifier.About, CenturyAndDecade(century, decade)) =>
          FuzzyDateRange(
            Year(century * 100 + (decade - 1) * 10),
            Year(century * 100 + (decade + 1) * 10),
          )
      }
    }
}
