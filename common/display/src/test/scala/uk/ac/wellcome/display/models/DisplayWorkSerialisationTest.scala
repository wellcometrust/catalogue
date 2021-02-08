package uk.ac.wellcome.display.models

import org.scalatest.funspec.AnyFunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.{
  ImageGenerators,
  ProductionEventGenerators,
  SubjectGenerators,
  WorkGenerators
}
import uk.ac.wellcome.models.work.internal.Format.{Books, EBooks}
import uk.ac.wellcome.models.work.internal._
import Implicits._

class DisplayWorkSerialisationTest
    extends AnyFunSpec
    with DisplaySerialisationTestBase
    with JsonMapperTestUtil
    with ProductionEventGenerators
    with SubjectGenerators
    with WorkGenerators
    with ImageGenerators {

  it("serialises a DisplayWork") {
    val work = indexedWork()
      .format(Books)
      .description(randomAlphanumeric(100))
      .lettering(randomAlphanumeric(100))
      .createdDate(Period("1901"))

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "description": "${work.data.description.get}",
      | "workType" : ${format(work.data.format.get)},
      | "lettering": "${work.data.lettering.get}",
      | "alternativeTitles": [],
      | "createdDate": ${period(work.data.createdDate.get)},
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(DisplayWork(work), expectedJson = expectedJson)
  }

  it("renders an item if the items include is present") {
    val work = indexedWork()
      .items(createIdentifiedItems(count = 1) :+ createUnidentifiableItemWith())

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "items": [ ${items(work.data.items)} ],
      | "availableOnline": true
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Items)),
      expectedJson = expectedJson
    )
  }

  it("includes 'items' if the items include is present, even with no items") {
    val work = indexedWork().items(Nil)

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "items": [ ],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Items)),
      expectedJson = expectedJson
    )
  }

  it("includes credit information in DisplayWork serialisation") {
    val location = DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = "",
      credit = Some("Wellcome Collection"),
      license = Some(License.CCBY)
    )
    val item = createIdentifiedItemWith(locations = List(location))
    val workWithCopyright = indexedWork().items(List(item))

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${workWithCopyright.state.canonicalId}",
      | "title": "${workWithCopyright.data.title.get}",
      | "alternativeTitles": [],
      | "items": [
      |   {
      |     "id": "${item.id.canonicalId}",
      |     "type": "Item",
      |     "locations": [
      |       {
      |         "type": "DigitalLocation",
      |         "url": "",
      |         "locationType": ${locationType(location.locationType)},
      |         "license": ${license(location.license.get)},
      |         "credit": "${location.credit.get}",
      |         "accessConditions" : []
      |       }
      |     ]
      |   }
      | ],
      | "availableOnline": true
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(
        workWithCopyright,
        includes = WorksIncludes(WorkInclude.Items)
      ),
      expectedJson = expectedJson
    )
  }

  it(
    "includes subject information in DisplayWork serialisation with the subjects include") {
    val workWithSubjects = indexedWork().subjects(
      (1 to 3).map(_ => createSubject).toList
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${workWithSubjects.state.canonicalId}",
      | "title": "${workWithSubjects.data.title.get}",
      | "alternativeTitles": [],
      | "subjects": [${subjects(workWithSubjects.data.subjects)}],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(
        workWithSubjects,
        includes = WorksIncludes(WorkInclude.Subjects)
      ),
      expectedJson = expectedJson
    )
  }

  it(
    "includes production information in DisplayWork serialisation with the production include") {
    val workWithProduction = indexedWork().production(
      createProductionEventList(count = 3)
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${workWithProduction.state.canonicalId}",
      | "title": "${workWithProduction.data.title.get}",
      | "alternativeTitles": [],
      | "production": [${production(workWithProduction.data.production)}],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(
        workWithProduction,
        includes = WorksIncludes(WorkInclude.Production)
      ),
      expectedJson = expectedJson
    )
  }

  it(
    "includes the contributors in DisplayWork serialisation with the contribuotrs include") {
    val work = indexedWork()
      .format(EBooks)
      .description(randomAlphanumeric(100))
      .lettering(randomAlphanumeric(100))
      .createdDate(Period("1901"))
      .contributors(
        List(
          Contributor(agent = Agent(randomAlphanumeric(25)), roles = Nil)
        )
      )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "description": "${work.data.description.get}",
      | "alternativeTitles": [],
      | "workType" : ${format(work.data.format.get)},
      | "lettering": "${work.data.lettering.get}",
      | "createdDate": ${period(work.data.createdDate.get)},
      | "contributors": [${contributor(work.data.contributors.head)}],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Contributors)),
      expectedJson = expectedJson
    )
  }

  it(
    "includes genre information in DisplayWork serialisation with the genres include") {
    val work = indexedWork().genres(
      List(
        Genre(
          label = "genre",
          concepts = List(Concept("woodwork"), Concept("etching"))
        )
      )
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "genres": [ ${genres(work.data.genres)} ],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Genres)),
      expectedJson = expectedJson
    )
  }

  it(
    "includes 'notes' if the notes include is present, with similar notes grouped together") {
    val work = indexedWork().notes(
      List(GeneralNote("A"), FundingInformation("B"), GeneralNote("C"))
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "notes": [
      |   {
      |     "noteType": {
      |       "id": "general-note",
      |       "label": "Notes",
      |       "type": "NoteType"
      |     },
      |     "contents": ["A", "C"],
      |     "type": "Note"
      |   },
      |   {
      |     "noteType": {
      |       "id": "funding-info",
      |       "label": "Funding information",
      |       "type": "NoteType"
      |     },
      |     "contents": ["B"],
      |     "type": "Note"
      |   }
      | ],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Notes)),
      expectedJson = expectedJson
    )
  }

  it("includes a list of identifiers on DisplayWork") {
    val otherIdentifier = createSourceIdentifier
    val work = indexedWork().otherIdentifiers(List(otherIdentifier))

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "identifiers": [
      |   ${identifier(work.sourceIdentifier)},
      |   ${identifier(otherIdentifier)}
      | ],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Identifiers)),
      expectedJson = expectedJson
    )
  }

  it("always includes 'identifiers' with the identifiers include") {
    val work = indexedWork().otherIdentifiers(Nil)

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "identifiers": [ ${identifier(work.sourceIdentifier)} ],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Identifiers)),
      expectedJson = expectedJson
    )
  }

  it("includes image stubs with the images include") {
    val work = indexedWork().imageData(
      (1 to 3).map(_ => createImageData.toIdentified).toList
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "images": [${workImageIncludes(work.data.imageData)}],
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes(WorkInclude.Images)),
      expectedJson
    )
  }

  it("shows the thumbnail field if available") {
    val work = indexedWork().thumbnail(
      DigitalLocation(
        locationType = LocationType("thumbnail-image"),
        url = "https://iiif.example.org/1234/default.jpg",
        license = Some(License.CCBY)
      )
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.state.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "thumbnail": ${location(work.data.thumbnail.get)},
      | "availableOnline": false
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWork(work, includes = WorksIncludes.none),
      expectedJson = expectedJson
    )
  }
}
