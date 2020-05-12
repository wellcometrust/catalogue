package uk.ac.wellcome.display.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import uk.ac.wellcome.models.work.internal.AugmentedImage

@Schema(
  name = "Image",
  description = "An image"
)
case class DisplayImage(
  @Schema(
    accessMode = Schema.AccessMode.READ_ONLY,
    description = "The canonical identifier given to a thing."
  ) id: String,
  @Schema(
    `type` = "uk.ac.wellcome.Display.models.DisplayDigitalLocation",
    description = "The locations which provide access to the image"
  ) locations: Seq[DisplayDigitalLocation],
  @Schema(
    `type` = "uk.ac.wellcone.Display.models.DisplayImageSource",
    description = "A description of the image's source"
  ) source: DisplayImageSource,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "Image"
)

object DisplayImage {

  def apply(image: AugmentedImage): DisplayImage =
    new DisplayImage(
      id = image.id.canonicalId,
      locations = Seq(DisplayDigitalLocation(image.location)),
      source = DisplayImageSource(image.source)
    )

}
