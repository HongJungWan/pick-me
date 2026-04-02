#!/bin/bash
# Debezium Outbox CDC 커넥터 등록 스크립트
# Kafka Connect가 기동된 후 실행

CONNECT_URL="http://kafka-connect:8083"

echo "Waiting for Kafka Connect..."
until curl -s "$CONNECT_URL/connectors" > /dev/null 2>&1; do
  sleep 2
done
echo "Kafka Connect is ready."

# 모듈별 Outbox CDC 커넥터 등록
MODULES=("order" "payment" "product" "inventory" "member" "partner" "notification" "settlement")

for MODULE in "${MODULES[@]}"; do
  SCHEMA="${MODULE}_schema"
  CONNECTOR_NAME="outbox-${MODULE}-connector"
  TOPIC="pickme.${MODULE}.events"

  echo "Registering connector: ${CONNECTOR_NAME}"

  curl -s -X POST "$CONNECT_URL/connectors" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"${CONNECTOR_NAME}\",
      \"config\": {
        \"connector.class\": \"io.debezium.connector.postgresql.PostgresConnector\",
        \"database.hostname\": \"postgres\",
        \"database.port\": \"5432\",
        \"database.user\": \"pickme\",
        \"database.password\": \"pickme1234\",
        \"database.dbname\": \"pickme_db\",
        \"database.server.name\": \"pickme\",
        \"schema.include.list\": \"${SCHEMA}\",
        \"table.include.list\": \"${SCHEMA}.outbox_events\",
        \"transforms\": \"outbox\",
        \"transforms.outbox.type\": \"io.debezium.transforms.outbox.EventRouter\",
        \"transforms.outbox.table.field.event.id\": \"event_id\",
        \"transforms.outbox.table.field.event.key\": \"aggregate_id\",
        \"transforms.outbox.table.field.event.type\": \"event_type\",
        \"transforms.outbox.table.field.event.payload\": \"payload\",
        \"transforms.outbox.route.topic.replacement\": \"${TOPIC}\",
        \"transforms.outbox.table.expand.json.payload\": \"true\",
        \"topic.prefix\": \"pickme\",
        \"plugin.name\": \"pgoutput\",
        \"slot.name\": \"outbox_${MODULE}_slot\",
        \"publication.name\": \"outbox_${MODULE}_pub\"
      }
    }"

  echo ""
done

echo "All connectors registered."
