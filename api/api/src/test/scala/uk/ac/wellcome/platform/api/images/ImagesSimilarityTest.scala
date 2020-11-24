package uk.ac.wellcome.platform.api.images

class ImagesSimilarityTest extends ApiImagesTestBase {

  it(
    "includes visually similar images on a single image if we pass ?include=visuallySimilar") {
    withImagesApi {
      case (imagesIndex, routes) =>
        val images =
          createSimilarImages(6, similarFeatures = true, similarPalette = true)
        val image = images.head
        insertImagesIntoElasticsearch(imagesIndex, images: _*)
        assertJsonResponse(
          routes,
          s"/$apiPrefix/images/${images.head.id.canonicalId}?include=visuallySimilar",
          unordered = true) {
          Status.OK ->
            s"""
               |{
               |  $singleImageResult,
               |  "id": "${image.id.canonicalId}",
               |  "locations": [${digitalLocation(image.locations)}],
               |  "visuallySimilar": [
               |    ${images.tail.map(imageResponse).mkString(",")}
               |  ],
               |  "source": ${imageSource(image.source)}
               |}""".stripMargin
        }
    }
  }

  it(
    "includes images with similar features on a single image if we pass ?include=withSimilarFeatures") {
    withImagesApi {
      case (imagesIndex, routes) =>
        val images =
          createSimilarImages(6, similarFeatures = true, similarPalette = false)
        val image = images.head
        insertImagesIntoElasticsearch(imagesIndex, images: _*)
        assertJsonResponse(
          routes,
          s"/$apiPrefix/images/${images.head.id.canonicalId}?include=withSimilarFeatures",
          unordered = true) {
          Status.OK ->
            s"""
               |{
               |  $singleImageResult,
               |  "id": "${image.id.canonicalId}",
               |  "locations": [${digitalLocation(image.locations)}],
               |  "withSimilarFeatures": [
               |    ${images.tail.map(imageResponse).mkString(",")}
               |  ],
               |  "source": ${imageSource(image.source)}
               |}""".stripMargin
        }
    }
  }

  it(
    "includes images with similar color palettes on a single image if we pass ?include=withSimilarColors") {
    withImagesApi {
      case (imagesIndex, routes) =>
        val images =
          createSimilarImages(6, similarFeatures = false, similarPalette = true)
        val image = images.head
        insertImagesIntoElasticsearch(imagesIndex, images: _*)
        assertJsonResponse(
          routes,
          s"/$apiPrefix/images/${images.head.id.canonicalId}?include=withSimilarColors",
          unordered = true) {
          Status.OK ->
            s"""
               |{
               |  $singleImageResult,
               |  "id": "${image.id.canonicalId}",
               |  "locations": [${digitalLocation(image.locations)}],
               |  "withSimilarColors": [
               |    ${images.tail.map(imageResponse).mkString(",")}
               |  ],
               |  "source": ${imageSource(image.source)}
               |}""".stripMargin
        }
    }
  }

  it("never includes visually similar images on an images search") {
    withImagesApi {
      case (imagesIndex, routes) =>
        val focacciaImage = createAugmentedImageWith(
          parentWork =
            identifiedWork().title("A Ligurian style of bread, Focaccia")
        )
        insertImagesIntoElasticsearch(imagesIndex, focacciaImage)

        assertJsonResponse(
          routes,
          s"/$apiPrefix/images?query=focaccia&include=visuallySimilar") {
          Status.BadRequest -> badRequest(
            apiPrefix,
            "include: 'visuallySimilar' is not a valid value. Please choose one of: ['source.contributors', 'source.languages']")
        }
    }
  }

}
