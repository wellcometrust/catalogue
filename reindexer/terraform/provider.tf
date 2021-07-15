provider "aws" {
  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"
  }
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
