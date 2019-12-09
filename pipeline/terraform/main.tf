module "catalogue_pipeline_20191115" {
  source = "./stack"

  namespace = "catalogue-20191115"

  release_label = "prod"

  account_id      = "${data.aws_caller_identity.current.account_id}"
  aws_region      = "${local.aws_region}"
  vpc_id          = "${local.vpc_id}"
  subnets         = ["${local.private_subnets}"]
  private_subnets = ["${local.private_subnets}"]

  dlq_alarm_arn = "${local.dlq_alarm_arn}"

  # Transformer config
  #
  # If this pipeline is meant to be reindexed, remember to uncomment the
  # reindexer topic names.

  sierra_adapter_topic_names = [
    #"${local.sierra_reindexer_topic_name}",
    "${local.sierra_merged_bibs_topic_name}",

    "${local.sierra_merged_items_topic_name}",
  ]
  sierra_adapter_topic_count = "3"
  miro_adapter_topic_names = [
    #"${local.miro_reindexer_topic_name}",
    "${local.miro_updates_topic_name}",
  ]
  miro_adapter_topic_count = "2"

  mets_adapter_topic_count = 1
  mets_adapter_topic_names = ["${local.mets_adapter_topic_name}"]

  # Elasticsearch
  es_works_index = "v2-20191115"
  # RDS
  rds_ids_access_security_group_id = "${local.rds_access_security_group_id}"
  # Adapter VHS
  vhs_sierra_read_policy            = "${local.vhs_sierra_read_policy}"
  vhs_miro_read_policy              = "${local.vhs_miro_read_policy}"
  vhs_sierra_sourcedata_bucket_name = "${local.vhs_sierra_sourcedata_bucket_name}"
  vhs_sierra_sourcedata_table_name  = "${local.vhs_sierra_sourcedata_table_name}"

  vhs_mets_adapter_bucket_name = "${local.vhs_mets_adapter_bucket_name}"
  vhs_mets_adapter_read_policy = "${local.vhs_mets_adapter_read_policy}"
  vhs_mets_adapter_table_name = "${local.vhs_mets_adapter_table_name}"
}

module "catalogue_pipeline_20191128" {
  source = "./stack"

  namespace = "catalogue-20191128"

  release_label = "latest"

  account_id      = "${data.aws_caller_identity.current.account_id}"
  aws_region      = "${local.aws_region}"
  vpc_id          = "${local.vpc_id}"
  subnets         = ["${local.private_subnets}"]
  private_subnets = ["${local.private_subnets}"]

  dlq_alarm_arn = "${local.dlq_alarm_arn}"

  # Transformer config
  #
  # If this pipeline is meant to be reindexed, remember to uncomment the
  # reindexer topic names.

  sierra_adapter_topic_names = [
    "${local.sierra_reindexer_topic_name}",
   # "${local.sierra_merged_bibs_topic_name}",

   # "${local.sierra_merged_items_topic_name}",
  ]
  sierra_adapter_topic_count = "1"
  miro_adapter_topic_names = [
    "${local.miro_reindexer_topic_name}",
   # "${local.miro_updates_topic_name}",
  ]
  miro_adapter_topic_count = "1"

  mets_adapter_topic_count = 1
  mets_adapter_topic_names = ["${local.mets_vhs_keys_topic_name}"]

  # Elasticsearch
  es_works_index = "v2-20191128"
  # RDS
  rds_ids_access_security_group_id = "${local.rds_access_security_group_id}"
  # Adapter VHS
  vhs_sierra_read_policy            = "${local.vhs_sierra_read_policy}"
  vhs_sierra_sourcedata_bucket_name = "${local.vhs_sierra_sourcedata_bucket_name}"
  vhs_sierra_sourcedata_table_name  = "${local.vhs_sierra_sourcedata_table_name}"
  vhs_miro_read_policy              = "${local.vhs_miro_read_policy}"

  vhs_mets_adapter_bucket_name = "${local.vhs_mets_adapter_bucket_name}"
  vhs_mets_adapter_read_policy = "${local.vhs_mets_adapter_read_policy}"
  vhs_mets_adapter_table_name = "${local.vhs_mets_adapter_table_name}"
}
