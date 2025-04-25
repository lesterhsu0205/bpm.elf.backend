#!/usr/bin/env bash
set -euo pipefail

# deploy.sh - 部署 bpm-elf-backend Swarm Stack，並根據 --local 前綴本機測試路徑
# Usage:
#   sudo ./deploy.sh [-r|--replicas N] [--local]

# 預設參數
REPLICAS=2
LOCAL_PREFIX=""
COMPOSE_FILE="docker-compose.dev.yml"
APP_NAME="bpm-elf-backend"

# 用法說明
usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  -r, --replicas N   指定 replicas 數量 (default: $REPLICAS)
      --local         在所有宿主機路徑前加上 /mockEnv 前綴 (不做 chown)
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
      LOCAL_PREFIX="mockEnv"
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
echo "➤ Deploying stack '$APP_NAME' with $REPLICAS replicas (local prefix: '$LOCAL_PREFIX')..."

# Step 1: 建立基底目錄
BASE_DIRS=(
  "${LOCAL_PREFIX}/opt/sw/tomcat"
  "${LOCAL_PREFIX}/data/${APP_NAME}"
  "${LOCAL_PREFIX}/opt/apps/${APP_NAME}"
)
for d in "${BASE_DIRS[@]}"; do
  if [ ! -d "$d" ]; then
    mkdir -p "$d"
    # local 模式不做 chown，否則以 wasadmin 擁有
    if [ -z "$LOCAL_PREFIX" ]; then
      chown wasadmin:was "$d"
      echo "  • Created & chown → $d"
    else
      echo "  • Created → $d"
    fi
  fi
done

# Step 2: 建 logs 子目錄
for ((i=1; i<=REPLICAS; i++)); do
  LOG_DIR="${LOCAL_PREFIX}/logs/${APP_NAME}.$i"
  if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
    if [ -z "$LOCAL_PREFIX" ]; then
      chown wasadmin:was "$LOG_DIR"
      echo "  • Created & chown → $LOG_DIR"
    else
      echo "  • Created → $LOG_DIR"
    fi
  fi
done

# Step 3: 部署 Swarm Stack
# 確保 docker-compose.dev.yml 裡 deploy.replicas: ${REPLICAS:-2} 用到環境變數
export REPLICAS

docker stack deploy -c "$COMPOSE_FILE" "$APP_NAME" --detach=false

echo "✅ Stack '$APP_NAME' deployed with $REPLICAS replicas."
