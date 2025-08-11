# BPM ELF Backend API

## 專案概述

BPM ELF Backend API 是一個基於 Spring Boot 的企業級業務流程管理（BPM）模板處理系統。本系統專為自動化IT需求單管理而設計，提供動態表單生成、模板管理、JSON處理等核心功能，支援企業內部各種IT服務申請流程的標準化和自動化處理。

## 核心功能特色

### 🎯 智能模板系統
- **動態JSON模板處理**：支援 `$enum` 和 `$include` 語法糖，實現模板的模組化和重複使用
- **基本資料合併**：自動識別重複欄位並合併至「基本資料」區塊，提高表單填寫效率
- **多層級模板引用**：支援最多兩層的模板嵌套引用，防止無窮遞迴
- **組合需求單**：支援複雜的多步驟工作流程模板

### 🔄 高級JSON處理引擎
- **遞歸合併處理**：智能處理JSON中的 `$include` 引用，實現模板組合
- **枚舉值動態替換**：透過 `$enum` 語法自動替換為預定義的下拉選項或單選按鈕
- **安全性驗證**：防止目錄遍歷攻擊和無效檔案類型上傳
- **循環引用檢測**：避免模板間的循環引用造成系統錯誤

### 📊 動態表單系統
- **多種輸入類型**：支援文字輸入、下拉選單、單選按鈕、多行文字等
- **模板變數替換**：支援 `${變數名}` 語法的動態內容替換
- **表單驗證**：內建表單欄位驗證機制

### 🗂️ 企業級檔案管理
- **模板CRUD操作**：完整的模板建立、讀取、更新、刪除功能
- **檔案系統整合**：與本地檔案系統無縫整合，支援熱更新
- **版本控制友好**：支援Git版本控制的檔案格式

## 技術架構

### 核心技術棧
- **後端框架**：Spring Boot 3.4.3
- **Java版本**：OpenJDK 17
- **建置工具**：Maven 3.x
- **JSON處理**：Jackson 2.x
- **應用伺服器**：Apache Tomcat（內嵌式）
- **容器化**：Docker + Docker Compose

### 系統架構
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend UI   │◄──►│ REST API Layer  │◄──►│ Template Engine │
│   (Angular/Vue) │    │ (Spring MVC)    │    │ (JSON Processor)│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │ Business Logic  │◄──►│ File Management │
                       │ (Service Layer) │    │ (Local Storage) │
                       └─────────────────┘    └─────────────────┘
```

## 專案結構詳解

```
bpm.elf.backend/
├── src/main/java/com/line/bank/bxi/bpm/elf/backend/
│   ├── Application.java                        # Spring Boot 主程序
│   ├── controller/
│   │   └── TemplateController.java            # REST API 控制器
│   ├── service/
│   │   └── TicketProcessorService.java        # 核心業務邏輯服務
│   ├── model/
│   │   └── TicketInputRef.java               # 票據輸入參考模型
│   └── constant/                              # 業務常數和枚舉
│       ├── ComponentEnum.java                 # 組件枚舉介面
│       ├── EnumRegistry.java                 # 枚舉註冊中心
│       ├── Dropdown.java                     # 下拉選單枚舉
│       ├── Radio.java                        # 單選按鈕枚舉
│       └── Text.java                         # 文字輸入枚舉
├── src/main/resources/
│   ├── application.yml                        # 應用程式配置
│   ├── logback-spring.xml                    # 日誌配置
│   └── templates/                            # JSON 模板檔案庫
│       ├── bitbucket.json                    # Bitbucket 帳號申請模板
│       ├── firewall.json                     # 防火牆規則申請模板
│       ├── server-auth.json                  # 伺服器權限申請模板
│       ├── compose/                          # 組合需求單模板
│       │   ├── apply-db.json                 # 資料庫申請流程
│       │   └── on-board.json                 # 到職流程
│       └── ...                               # 其他業務模板
├── mockEnv/                                  # 模擬部署環境
├── docker-compose.yml                        # Docker 編排檔案
└── pom.xml                                   # Maven 專案配置
```

## REST API 端點說明

### 模板管理 API

#### 📋 取得所有模板列表
```http
GET /bpm-elf/api/settings
```
返回處理過的模板列表（已合併 $include 和 $enum）

#### 🔍 取得原始模板列表
```http
GET /bpm-elf/api/settings-raw
```
返回未處理的原始模板內容

#### 📄 取得指定模板
```http
GET /bpm-elf/api/setting/{filename}
```
取得單一模板的完整內容（已處理所有語法糖）

#### 📦 取得組合模板
```http
GET /bpm-elf/api/setting/compose/{filename}
```
取得位於 compose 目錄下的複雜工作流程模板

#### ✍️ 建立或更新模板
```http
POST /bpm-elf/api/setting/{filename}
Content-Type: application/json

{
  "name": "新模板",
  "path": ["分類", "子分類"],
  "inputs": [...]
}
```

#### 🗑️ 刪除模板
```http
DELETE /bpm-elf/api/setting/{filename}
```

### 功能型 API

#### 🔄 預渲染資料
```http
POST /bpm-elf/api/prerenderData
Content-Type: application/json

{
  "newFileName": "template.json",
  "jsonData": {...}
}
```
用於前端預覽功能，處理模板變數替換

#### 🧭 取得動態側邊欄
```http
GET /bpm-elf/api/sidebar
```
根據模板的 path 屬性動態生成階層式側邊欄選單

## 模板語法說明

### 基本模板結構
```json
{
  "name": "模板名稱",
  "path": ["主分類", "子分類", "項目名稱"],
  "inputs": [
    {
      "type": "text",
      "label": "顯示標籤",
      "key": "欄位鍵值"
    }
  ]
}
```

### 枚舉語法糖 (`$enum`)
```json
{
  "$enum": "Dropdown.DEPARTMENT"
}
```
自動替換為預定義的下拉選單或單選按鈕配置

### 引用語法糖 (`$include`)
```json
{
  "$include": "common-fields.json"
}
```
引用其他模板的內容，實現模組化設計

### 支援的輸入類型
- `text`: 單行文字輸入
- `textarea`: 多行文字輸入
- `dropdown`: 下拉選單
- `radio`: 單選按鈕
- `description`: 描述文字（支援模板變數）

## 快速開始

### 環境要求
- Java 17 或更高版本
- Maven 3.6+
- Docker（可選，用於容器化部署）
- 磁碟空間：至少 500MB

### 本地開發設定

#### 1. 複製並建置專案
```bash
git clone <repository-url>
cd bpm.elf.backend

# 建置專案（包含 Docker 映像檔）
./mvnw clean package

# 或僅建置 JAR/WAR 檔案
./mvnw clean package -Dskip.docker=true
```

#### 2. 準備模板目錄
```bash
# 建立模板檔案目錄
sudo mkdir -p /data/bpm-elf-backend/templates

# 複製範例模板（開發環境）
cp -r src/main/resources/templates/* /data/bpm-elf-backend/templates/
```

#### 3. 啟動應用程式
```bash
# 方式一：直接執行 WAR 檔案
java -jar target/bpm-elf-backend-0.0.2-SNAPSHOT.war

# 方式二：使用 Docker
docker run -p 8080:8080 \
  -v /data/bpm-elf-backend:/data/bpm-elf-backend \
  bpm-elf-backend:0.0.2-SNAPSHOT

# 方式三：使用 Docker Compose
docker-compose up -d
```

#### 4. 驗證部署
```bash
# 健康檢查
curl http://localhost:8080/actuator/health

# 測試API
curl http://localhost:8080/bpm-elf/api/settings
```

### 開發模式設定

#### IDE 配置建議
- IntelliJ IDEA：啟用 Spring Boot 自動重載
- VS Code：安裝 Java Extension Pack
- Eclipse：安裝 Spring Tools Suite

#### 開發時的環境變數
```bash
export HOSTNAME=dev-server
export JAVA_OPTS="-Xmx512m -Dspring.profiles.active=dev"
```

## 配置管理

### application.yml 主要配置項
```yaml
spring:
  application:
    name: com.line.bank.bxi.bpm.elf.backend

server:
  port: 8080                              # 服務埠號
  netty:
    connection-timeout: 30s               # 連接逾時
    max-http-header-size: 16KB           # HTTP 標頭大小限制

file:
  base:
    directory: /data/bpm-elf-backend/templates  # 模板檔案根目錄

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics      # 開放的監控端點
  endpoint:
    health:
      show-details: always               # 顯示詳細健康資訊
```

### Docker 環境變數支援
```bash
# 自定義模板目錄
TEMPLATE_DIR=/custom/path/templates

# JVM 參數調整
JAVA_OPTS="-Xmx1024m -Xms512m"

# 主機名稱（用於檔案路徑）
HOSTNAME=production-server
```

## 測試策略

### 單元測試
```bash
# 執行所有測試
./mvnw test

# 執行特定測試類別
./mvnw test -Dtest=TemplateControllerTest

# 測試涵蓋率報告
./mvnw jacoco:report
```

### 整合測試
```bash
# 啟動測試環境
docker-compose -f docker-compose.test.yml up -d

# 執行整合測試
./mvnw verify
```

### API 測試範例
```bash
# 測試模板列表 API
curl -X GET http://localhost:8080/bpm-elf/api/settings

# 測試模板上傳
curl -X POST http://localhost:8080/bpm-elf/api/setting/test.json \
  -H "Content-Type: application/json" \
  -d '{"name":"測試模板","inputs":[]}'
```

## 生產部署指南

### Docker 生產環境部署
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  bpm-elf-backend:
    image: bpm-elf-backend:0.0.2-SNAPSHOT
    ports:
      - "80:8080"
    environment:
      - JAVA_OPTS=-Xmx2048m -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=production
    volumes:
      - /production/templates:/data/bpm-elf-backend/templates:ro
      - /production/logs:/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 反向代理設定 (Nginx)
```nginx
upstream bpm-elf-backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name your-domain.com;
    
    location /bpm-elf/ {
        proxy_pass http://bpm-elf-backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 效能最佳化建議
- **JVM 參數調整**：`-Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200`
- **連接池設定**：適當配置 Tomcat 連接池大小
- **檔案快取**：實作模板檔案的記憶體快取機制
- **壓縮傳輸**：啟用 Gzip 壓縮減少網路傳輸

## 監控與維運

### 健康檢查端點
```bash
# 應用程式健康狀態
curl http://localhost:8080/actuator/health

# 系統資訊
curl http://localhost:8080/actuator/info

# 效能指標
curl http://localhost:8080/actuator/metrics
```

### 日誌管理
```yaml
# logback-spring.xml 配置
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>/logs/app.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.line.bank.bxi.bpm.elf.backend" level="INFO"/>
    <root level="WARN">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 監控指標
- **回應時間**：API 呼叫的平均回應時間
- **錯誤率**：HTTP 4xx/5xx 錯誤的比例
- **吞吐量**：每秒處理的請求數量
- **記憶體使用率**：JVM 堆積記憶體使用情況
- **檔案系統**：模板目錄的磁碟使用率

## 安全性考量

### 檔案安全
- **路徑驗證**：防止目錄遍歷攻擊（`../`）
- **檔案類型限制**：僅允許 `.json` 副檔名
- **檔案大小限制**：避免超大檔案上傳
- **權限控制**：模板目錄的適當檔案權限設定

### API 安全
- **輸入驗證**：所有用戶輸入都需要驗證
- **CORS 設定**：適當配置跨域存取規則
- **Rate Limiting**：實作 API 呼叫頻率限制
- **錯誤訊息**：避免洩露敏感系統資訊

## 疑難排解

### 常見問題

#### 1. 模板檔案讀取失敗
```bash
# 檢查檔案權限
ls -la /data/bpm-elf-backend/templates/

# 檢查目錄是否存在
mkdir -p /data/bpm-elf-backend/templates/
```

#### 2. Docker 容器啟動失敗
```bash
# 檢查容器日誌
docker logs bpm-elf-backend

# 檢查埠號衝突
netstat -tulpn | grep 8080
```

#### 3. JSON 解析錯誤
- 確認 JSON 格式正確
- 檢查 `$include` 檔案是否存在
- 驗證 `$enum` 引用的枚舉是否已定義

#### 4. 記憶體不足錯誤
```bash
# 調整 JVM 記憶體參數
export JAVA_OPTS="-Xmx2048m -Xms1024m"
```

### 除錯技巧
```bash
# 開啟除錯模式
java -jar app.jar --logging.level.com.line.bank.bxi.bpm.elf.backend=DEBUG

# 查看詳細的 HTTP 請求日誌
curl -v http://localhost:8080/bpm-elf/api/settings
```

## 版本資訊

### 當前版本：v0.0.2-SNAPSHOT

#### 主要特性
- ✅ 完整的模板 CRUD API
- ✅ 智能JSON處理引擎
- ✅ 動態側邊欄生成
- ✅ Docker 容器化支援
- ✅ 企業級安全防護

#### 版本歷程
- **v0.0.2**: 新增預渲染API、側邊欄API、安全性增強
- **v0.0.1**: 基礎模板管理功能、JSON處理引擎

## 開發貢獻

### 程式碼風格
- 遵循 Google Java Style Guide
- 使用 4 空格縮排，不使用 Tab
- 行長度限制：120 字符
- 必須提供 Javadoc 註解

### 提交規範
```bash
# 功能開發
git commit -m "feat: 新增模板批次上傳功能"

# 錯誤修復
git commit -m "fix: 修復JSON循環引用檢測邏輯"

# 文件更新
git commit -m "docs: 更新API文檔範例"
```

### 開發流程
1. Fork 專案並建立功能分支
2. 開發新功能並撰寫測試
3. 確保所有測試通過
4. 提交 Pull Request
5. 等待程式碼審查

## 授權資訊

本專案為企業內部使用，請遵守公司相關的程式碼使用規範。

---

## 技術支援

如遇到問題或需要技術支援，請聯絡：
- **開發團隊**：BPM ELF 開發小組
- **維護負責人**：[聯絡資訊]
- **文檔更新**：2025-08-11

> 💡 **提示**：建議在生產環境部署前，先在測試環境完整驗證所有功能。