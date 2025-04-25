#!/usr/bin/env bash
set -euo pipefail

# shutdown.sh - 優雅關閉 bpm-elf-backend Stack，並在需要時清理自訂網路
# Usage: sudo ./shutdown.sh

STACK_NAME="bpm-elf-backend"
DEFAULT_NET="${STACK_NAME}_default"

echo "➤ Shutting down stack '${STACK_NAME}'..."
# 移除 Stack（連同其下所有 Service/Container）
docker stack rm "${STACK_NAME}"

# 等待 Stack 真正移除（直到 docker stack ls 不再包含該名稱）
echo -n "  • Waiting for stack removal"
while docker stack ls --format '{{.Name}}' | grep -wq "${STACK_NAME}"; do
  echo -n "."
  sleep 1
done
echo " done."

# 清理 Stack 預設建立的 overlay network（若仍在）
if docker network inspect "${DEFAULT_NET}" >/dev/null 2>&1; then
  echo "  • Removing network '${DEFAULT_NET}'..."
  docker network rm "${DEFAULT_NET}" || echo "    ↳ Network not found or already removed, skipping"
fi

echo "✅ Stack '${STACK_NAME}' has been removed and cleanup completed."