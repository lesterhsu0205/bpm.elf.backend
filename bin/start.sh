#!/usr/bin/env bash
set -euo pipefail

# deploy.sh - 部署 bpm-elf-backend Swarm Stack，並根據 --local 是否要在本機測試
# Usage:
#   sudo ./deploy.sh [-r|--replicas N] [--local]

# 預設參數
REPLICAS=2
LOCAL_MODE=0
ROOT_DIR=""
COMPOSE_FILE="docker-compose.dev.yml"
APP_NAME="bpm-elf-backend"
VERSION=$(< version.txt)
# 去除首尾空白
VERSION=${VERSION##+([[:space:]])}
VERSION=${VERSION%%+([[:space:]])}

echo "version：$VERSION"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Error: Compose file '$COMPOSE_FILE' not found." >&2
  exit 1
fi

# 用法說明
usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  -r, --replicas N   指定 replicas 數量 (default: $REPLICAS)
      --local        本機測試
  -h, --help         顯示本說明並退出
EOF
  exit 1
}

# 參數解析
while [[ $# -gt 0 ]]; do
  case $1 in
    -r|--replicas)
      if [[ -n "${2-}" && "$2" =~ ^[0-9]+$ ]]; then
        REPLICAS="$2"
        shift 2
      else
        echo "Error: '--replicas' 需要一個整數參數。" >&2
        usage
      fi
      ;;
    --local)
      LOCAL_MODE=1
      ROOT_DIR="./mockEnv"
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      ;;
  esac
done

# 顯示啟動資訊
echo "➤ Deploying stack '$APP_NAME' with $REPLICAS replicas (local mode: $LOCAL_MODE)..."

if [[ $LOCAL_MODE -eq 1 ]]; then
  echo "  • Local mode: creating base and log directories under mockEnv"

  # Step 1: 建立基底目錄
  BASE_DIRS=(
    "$ROOT_DIR/opt/sw/${APP_NAME}/{host}-01/tomcat"
    "$ROOT_DIR/data/${APP_NAME}"
#    "$ROOT_DIR/opt/apps/${APP_NAME}"
  )

  for d in "${BASE_DIRS[@]}"; do
    if [[ ! -d "$d" ]]; then
      mkdir -p "$d"
      echo "  • Created → $d"
    fi
  done

  # Step 2: 建 logs 子目錄
  for ((i=1; i<=REPLICAS; i++)); do
    LOG_DIR="$ROOT_DIR/logs/${APP_NAME}/{host}-0$i"
    if [[ ! -d "$LOG_DIR" ]]; then
      mkdir -p "$LOG_DIR"
      echo "  • Created → $LOG_DIR"
    fi
  done
fi

# Step 3: 部署 Swarm Stack
# 確保 docker-compose.dev.yml 裡 deploy.replicas: ${REPLICAS:-2} 用到環境變數
export REPLICAS ROOT_DIR VERSION

docker stack deploy -c "$COMPOSE_FILE" "$APP_NAME" --detach=false

echo "✅ Stack '$APP_NAME' deployed with $REPLICAS replicas."
