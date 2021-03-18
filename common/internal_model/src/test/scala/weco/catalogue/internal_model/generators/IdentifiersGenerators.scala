package weco.catalogue.internal_model.generators

import uk.ac.wellcome.fixtures.RandomGenerators
import weco.catalogue.internal_model.identifiers.{
  IdentifierType,
  SourceIdentifier
}

import scala.util.Random

trait IdentifiersGenerators extends RandomGenerators {
  def createCanonicalId: String = randomAlphanumeric(length = 10).toLowerCase()

  def createSourceIdentifier: SourceIdentifier = createSourceIdentifierWith()

  def createSourceIdentifierWith(
    identifierType: IdentifierType = chooseFrom(
      IdentifierType("miro-image-number"),
      IdentifierType("sierra-system-number"),
      IdentifierType("calm-record-id")
    ),
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = identifierType,
      value = value,
      ontologyType = ontologyType
    )

  def createSierraSystemSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      value = value,
      ontologyType = ontologyType
    )

  def createMetsSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(identifierType = IdentifierType("mets"))

  def createSierraSystemSourceIdentifier: SourceIdentifier =
    createSierraSystemSourceIdentifierWith()

  def createSierraIdentifierSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      value = value,
      ontologyType = ontologyType
    )

  def createSierraIdentifierSourceIdentifier: SourceIdentifier =
    createSierraIdentifierSourceIdentifierWith()

  def createIsbnSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(
      identifierType = IdentifierType("isbn")
    )

  private val miroIdPrefixes: Seq[Char] = Seq(
    'C', 'L', 'V', 'W', 'N', 'M', 'B', 'A', 'S', 'F', 'D'
  )

  def randomMiroId(prefix: Char = chooseFrom(miroIdPrefixes: _*),
                   length: Int = 8): String =
    s"%c%0${length - 1}d".format(
      prefix,
      Random.nextInt(math.pow(10, length - 1).toInt)
    )

  def createMiroSourceIdentifierWith(
    value: String = randomMiroId(),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = ontologyType,
      value = value
    )

  def createMiroSourceIdentifier: SourceIdentifier =
    createMiroSourceIdentifierWith()

  def createCalmRecordID: String =
    randomUUID.toString

  def createCalmSourceIdentifier: SourceIdentifier =
    SourceIdentifier(
      value = createCalmRecordID,
      identifierType = IdentifierType("calm-record-id"),
      ontologyType = "Work"
    )

  def createDigcodeIdentifier(digcode: String): SourceIdentifier =
    SourceIdentifier(
      value = digcode,
      identifierType = IdentifierType("wellcome-digcode"),
      ontologyType = "Work"
    )
}