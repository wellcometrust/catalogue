package weco.pipeline.transformer.sierra.transformers.parsers

import fastparse._, NoWhitespace._

import weco.catalogue.internal_model.parse.Parser
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.{Period, ProductionEvent}

/**
  *  Parses Marc 008 fields into ProductionEvent
  *
  *  Spec: https://www.loc.gov/marc/bibliographic/bd008a.html
  */
object Marc008Parser extends Parser[ProductionEvent[IdState.Unminted]] {

  def parser[_: P] =
    (Start ~ createdDate ~ Marc008DateParser.parser ~ MarcPlaceParser.parser.?)
      .map {
        case (instantRange, place) =>
          ProductionEvent(
            label = instantRange.label,
            agents = Nil,
            dates = Period(instantRange.label, Some(instantRange)) :: Nil,
            places = place.toList,
            function = None)
      }

  def createdDate[_: P] = AnyChar.rep(exactly = 6)
}
