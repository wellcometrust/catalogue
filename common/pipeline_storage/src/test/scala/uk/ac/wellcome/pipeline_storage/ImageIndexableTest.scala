package uk.ac.wellcome.pipeline_storage

import com.sksamuel.elastic4s.Index
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.elasticsearch.ImagesIndexConfig
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.{
  ImageGenerators,
  InstantGenerators,
  WorkGenerators
}
import uk.ac.wellcome.models.work.internal.SourceWork._
import uk.ac.wellcome.models.work.internal.{Image, ImageState, SourceWorks}
import uk.ac.wellcome.pipeline_storage.Indexable.imageIndexable
import uk.ac.wellcome.pipeline_storage.fixtures.ElasticIndexerFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ImageIndexableTest
    extends AnyFunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with ElasticIndexerFixtures
    with ImageGenerators
    with WorkGenerators
    with InstantGenerators {

  describe("updating images with merged / redirected sources") {
    it("overrides an image if modifiedTime is higher") {
      val originalImage = createImageData.toAugmentedImage
      val updatedModifiedTimeImage = originalImage.copy(
        state = originalImage.state.copy(
          modifiedTime = originalImage.state.modifiedTime + (2 minutes)
        )
      )

      withImagesIndexAndIndexer {
        case (index, indexer) =>
          val insertFuture = ingestInOrder(indexer)(
            originalImage,
            updatedModifiedTimeImage
          )

          whenReady(insertFuture) { result =>
            assertIngestedImageIs(
              result = result,
              ingestedImage = updatedModifiedTimeImage,
              index = index
            )
          }
      }
    }
    it("does not override an image if modifiedTime is lower") {
      val originalImage = createImageData.toAugmentedImage
      val updatedModifiedTimeImage = originalImage.copy(
        state = originalImage.state.copy(
          modifiedTime = originalImage.state.modifiedTime - (2 minutes)
        )
      )

      withImagesIndexAndIndexer {
        case (index, indexer) =>
          val insertFuture = ingestInOrder(indexer)(
            originalImage,
            updatedModifiedTimeImage
          )

          whenReady(insertFuture) { result =>
            assertIngestedImageIs(
              result = result,
              ingestedImage = originalImage,
              index = index
            )
          }
      }
    }
    it("overrides an image if modifiedTime is the same") {
      val originalImage = createImageData.toAugmentedImage
      val updatedSourceImage = originalImage.copy(
        state = originalImage.state.copy(
          source = SourceWorks(sierraIdentifiedWork().toSourceWork, None)
        )
      )

      withImagesIndexAndIndexer {
        case (index, indexer) =>
          val insertFuture = ingestInOrder(indexer)(
            originalImage,
            updatedSourceImage
          )

          whenReady(insertFuture) { result =>
            assertIngestedImageIs(
              result = result,
              ingestedImage = updatedSourceImage,
              index = index
            )
          }
      }
    }
  }

  private def assertIngestedImageIs(
    result: Either[Seq[Image[ImageState.Augmented]],
                   Seq[Image[ImageState.Augmented]]],
    ingestedImage: Image[ImageState.Augmented],
    index: Index): Seq[Assertion] = {
    result.isRight shouldBe true
    assertElasticsearchEventuallyHasImage(index, ingestedImage)
  }

  private def withImagesIndexAndIndexer[R](
    testWith: TestWith[(Index, ElasticIndexer[Image[ImageState.Augmented]]),
                       R]) =
    withLocalImagesIndex { index =>
      val indexer = new ElasticIndexer(elasticClient, index, ImagesIndexConfig)
      testWith((index, indexer))
    }
}
