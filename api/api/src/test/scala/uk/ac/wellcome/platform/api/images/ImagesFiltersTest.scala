package uk.ac.wellcome.platform.api.images

import uk.ac.wellcome.models.work.internal.License
import uk.ac.wellcome.models.Implicits._

class ImagesFiltersTest extends ApiImagesTestBase {
  describe("filtering images by license") {
    val ccByImage = createLicensedImage(License.CCBY)
    val ccByNcImage = createLicensedImage(License.CCBYNC)

    it("filters by license") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(imagesIndex, ccByImage, ccByNcImage)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/images?locations.license=cc-by") {
            Status.OK -> imagesListResponse(
              images = Seq(ccByImage)
            )
          }
      }
    }
  }

  describe("filtering images by color") {
    val redImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "7/0",
            "7/0",
            "7/0",
            "71/1",
            "71/1",
            "71/1",
            "268/2",
            "268/2",
            "268/2",
          ))))
    val blueImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "9/0",
            "9/0",
            "9/0",
            "5/0",
            "74/1",
            "74/1",
            "74/1",
            "35/1",
            "50/1",
            "29/1",
            "38/1",
            "273/2",
            "273/2",
            "273/2",
            "187/2",
            "165/2",
            "115/2",
            "129/2",
          ))))
    val slightlyLessRedImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "7/0",
            "71/1",
            "71/1",
            "71/1",
          ))))
    val evenLessRedImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "7/0",
            "7/0",
            "7/0",
          ))))

    it("filters by color") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(imagesIndex, redImage, blueImage)
          assertJsonResponse(routes, f"/$apiPrefix/images?color=ff0000") {
            Status.OK -> imagesListResponse(
              images = Seq(redImage)
            )
          }
      }
    }

    it("filters by multiple colors") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(imagesIndex, redImage, blueImage)
          assertJsonResponse(
            routes,
            f"/$apiPrefix/images?color=ff0000,0000ff",
            unordered = true) {
            Status.OK -> imagesListResponse(
              images = Seq(blueImage, redImage)
            )
          }
      }
    }

    it("scores by number of color bin matches") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(
            imagesIndex,
            redImage,
            slightlyLessRedImage,
            evenLessRedImage,
            blueImage
          )
          assertJsonResponse(routes, f"/$apiPrefix/images?color=ff0000") {
            Status.OK -> imagesListResponse(
              images = Seq(redImage, slightlyLessRedImage, evenLessRedImage)
            )
          }
      }
    }
  }
}
