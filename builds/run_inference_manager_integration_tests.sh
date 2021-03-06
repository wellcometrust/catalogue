#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

ROOT=$(git rev-parse --show-toplevel)
BUILDS_DIR="$ROOT/builds"

for PROJECT in feature_inferrer palette_inferrer aspect_ratio_inferrer
do
  docker build \
    --file "$ROOT/pipeline/inferrer/$PROJECT/Dockerfile" \
    --tag "$PROJECT" \
    "$ROOT/pipeline/inferrer/$PROJECT"
done

$BUILDS_DIR/run_sbt_task_in_docker.sh \
  "project inference_manager" \
  ";dockerComposeUp;testOnly **.integration.*;dockerComposeStop"
