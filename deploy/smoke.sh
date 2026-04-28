#!/usr/bin/env bash
set -euo pipefail

retry() {
  local attempts=$1; shift
  local delay=$1; shift
  for _ in $(seq 1 "$attempts"); do
    if "$@"; then return 0; fi
    sleep "$delay"
  done
  return 1
}

echo "Waiting for app to be healthy..."
retry 60 2 curl -fsS http://localhost:8080/actuator/health >/dev/null

echo "Creating product..."
SKU="SKU-PIPELINE-$(date +%s%N | cut -c1-16)"
PRODUCT_JSON=$(printf '{"sku":"%s","name":"Demo product","price":10.00,"currency":"USD","stock":10}' "$SKU")

curl -fsS -X POST http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d "$PRODUCT_JSON" >/dev/null

echo "Querying product by sku..."
curl -fsS "http://localhost:8080/products?sku=$SKU" >/dev/null

echo "Smoke OK"
