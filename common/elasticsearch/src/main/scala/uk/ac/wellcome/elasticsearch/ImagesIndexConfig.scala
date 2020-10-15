package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl.{keywordField, _}
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.mappings.{MappingDefinition, ObjectField}
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping

object ImagesIndexConfig extends IndexConfig with WorksIndexConfigFields {

  override val analysis: Analysis = WorksAnalysis()

  val inferredData = objectField("inferredData").fields(
    denseVectorField("features1", 2048),
    denseVectorField("features2", 2048),
    keywordField("lshEncodedFeatures"),
    keywordField("palette"))

  private def sourceWork(canonicalWork: String): ObjectField =
    objectField(canonicalWork).fields(
      id(),
      data(textField("path"), id()),
      keywordField("type"),
      version
    )

  val source = objectField("source").fields(
    sourceWork("canonicalWork"),
    sourceWork("redirectedWork"),
    keywordField("type"),
    intField("numberOfSources")
  )

  override val mapping: MappingDefinition = properties(
    id(),
    version,
    location("location"),
    source,
    inferredData
  ).dynamic(DynamicMapping.Strict)
}
