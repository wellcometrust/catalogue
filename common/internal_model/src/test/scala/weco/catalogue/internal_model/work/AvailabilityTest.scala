package weco.catalogue.internal_model.work

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.locations.{AccessStatus, LocationType}
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}

class AvailabilityTest
    extends AnyFunSpec
    with Matchers
    with WorkGenerators
    with ItemsGenerators {
  describe("Availabilities.forWorkData") {
    it(
      "adds Availability.Online if there is a digital location with an Open, OpenWithAdvisory or LicensedResources access status") {
      val openWork = denormalisedWork().items(
        List(createDigitalItemWith(accessStatus = AccessStatus.Open)))
      val openWithAdvisoryWork = denormalisedWork().items(
        List(
          createDigitalItemWith(accessStatus = AccessStatus.OpenWithAdvisory)))
      val licensedResourcesWork = denormalisedWork().items(
        List(
          createDigitalItemWith(accessStatus = AccessStatus.LicensedResources)))
      val availabilities =
        List(openWork, openWithAdvisoryWork, licensedResourcesWork)
          .map(work => Availabilities.forWorkData(work.data))

      every(availabilities) should contain only Availability.Online
    }

    it(
      "adds Availability.InLibrary if there is an item with a physical location") {
      val work = denormalisedWork().items(List(createIdentifiedPhysicalItem))
      val workAvailabilities = Availabilities.forWorkData(work.data)

      workAvailabilities should contain only Availability.InLibrary
    }

    it("does not add Availability.InLibrary if the only location is OnOrder") {
      val work = denormalisedWork()
        .items(
          List(
            createIdentifiedItemWith(
              locations = List(
                createPhysicalLocationWith(
                  locationType = LocationType.OnOrder
                )
              ))
          )
        )
      val workAvailabilities = Availabilities.forWorkData(work.data)

      workAvailabilities shouldBe empty
    }

    it("does not add Availability.InLibrary if the location is offsite") {
      val work = denormalisedWork()
        .items(List(createIdentifiedPhysicalItem))
        .notes(
          List(TermsOfUse("Available at Churchill Archives Centre"))
        )
      val workAvailabilities = Availabilities.forWorkData(work.data)

      workAvailabilities shouldBe empty
    }

    it("adds Availability.InLibrary if there are locations other than OnOrder") {
      val work = denormalisedWork()
        .items(
          List(
            createIdentifiedItemWith(
              locations = List(
                createPhysicalLocationWith(
                  locationType = LocationType.OnOrder
                ),
                createPhysicalLocationWith(
                  locationType = LocationType.OpenShelves
                )
              ))
          )
        )
      val workAvailabilities = Availabilities.forWorkData(work.data)

      workAvailabilities should contain only Availability.InLibrary
    }

    describe("if there is a holdings") {
      it("with no physical location, then no availabilities") {
        val work = denormalisedWork()
          .holdings(
            List(
              Holdings(
                note = Some("A holdings in a mystery place"),
                enumeration = Nil,
                location = None
              )
            )
          )
        val workAvailabilities = Availabilities.forWorkData(work.data)

        workAvailabilities shouldBe empty
      }

      it("with a physical location, then it adds Availability.InLibrary") {
        val work = denormalisedWork()
          .holdings(
            List(
              Holdings(
                note = Some("A holdings in the closed stores"),
                enumeration = Nil,
                location = Some(createPhysicalLocation)
              )
            )
          )
        val workAvailabilities = Availabilities.forWorkData(work.data)

        workAvailabilities shouldBe Set(Availability.InLibrary)
      }
    }

    it(
      "adds Availability.Online and Availability.InLibrary if the conditions for both are satisfied") {
      val work = denormalisedWork().items(
        List(
          createIdentifiedPhysicalItem,
          createDigitalItemWith(accessStatus = AccessStatus.Open)))
      val workAvailabilities = Availabilities.forWorkData(work.data)

      workAvailabilities should contain allOf (Availability.InLibrary, Availability.Online)
    }

    it("does not add either availability if no conditions are satisfied") {
      val work = denormalisedWork().items(
        List(createDigitalItemWith(accessStatus = AccessStatus.Closed)))
      val workAvailabilities = Availabilities.forWorkData(work.data)

      workAvailabilities.size shouldBe 0
    }
  }
}
