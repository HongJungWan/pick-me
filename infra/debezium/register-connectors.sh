#!/bin/bash
# Debezium Outbox CDC 커넥터 등록 스크립트
# 단일 커넥터로 8개 스키마의 outbox_events 테이블을 통합 캡처
# Kafka Connect가 기동된 후 실행

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Waiting for Kafka Connect at ${CONNECT_URL}..."
until curl -s "${CONNECT_URL}/connectors" > /dev/null 2>&1; do
  sleep 2
done
echo "Kafka Connect is ready."

# 기존 모듈별 커넥터가 있으면 삭제 (마이그레이션 전환용)
MODULES=("order" "payment" "product" "inventory" "member" "partner" "notification" "settlement")
for MODULE in "${MODULES[@]}"; do
  OLD_CONNECTOR="outbox-${MODULE}-connector"
  if curl -s "${CONNECT_URL}/connectors/${OLD_CONNECTOR}" > /dev/null 2>&1; then
    echo "Removing legacy connector: ${OLD_CONNECTOR}"
    curl -s -X DELETE "${CONNECT_URL}/connectors/${OLD_CONNECTOR}"
    echo ""
  fi
done

# 단일 통합 커넥터 등록
CONNECTOR_NAME="pickme-outbox-connector"

# 이미 존재하면 업데이트, 없으면 생성
if curl -s "${CONNECT_URL}/connectors/${CONNECTOR_NAME}" > /dev/null 2>&1; then
  echo "Updating existing connector: ${CONNECTOR_NAME}"
  curl -s -X PUT "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/config" \
    -H "Content-Type: application/json" \
    -d "$(jq '.config' "${SCRIPT_DIR}/pickme-outbox-connector.json")"
else
  echo "Registering new connector: ${CONNECTOR_NAME}"
  curl -s -X POST "${CONNECT_URL}/connectors" \
    -H "Content-Type: application/json" \
    -d @"${SCRIPT_DIR}/pickme-outbox-connector.json"
fi

echo ""
echo "Connector status:"
curl -s "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status" | jq .

echo ""
echo "Done."
