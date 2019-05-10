module "vhs_sierra" {
  source = "./vhs"
  name   = "sourcedata-sierra"

  table_read_max_capacity  = 500
  table_write_max_capacity = 500
  account_id               = "${data.aws_caller_identity.current.account_id}"

  s3_cross_account_access_accounts = ["269807742353"]
}
