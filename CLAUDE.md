# CLAUDE.md

此檔案為 Claude Code (claude.ai/code) 在此儲存庫中工作時提供指引。

## 必要指令

### 建置與打包
```bash
# 完整建置（包含 Docker 映像檔）
./mvnw clean package

# 僅建置 WAR 檔案（開發時較快）
./mvnw clean package -Dskip.docker=true

# 建置前清除舊的 Docker 映像檔
./mvnw pre-clean
```

### 開發伺服器
```bash
# 直接執行 Spring Boot 應用程式
java -jar target/bpm-elf-backend-0.0.2-SNAPSHOT.war

# 使用 Docker 執行
docker run -p 8080:8080 -v /data/bpm-elf-backend:/data/bmp-elf-backend bpm-elf-backend:0.0.2-SNAPSHOT

# 使用 Docker Compose 執行
docker-compose up -d
```

### 測試
```bash
# 執行所有測試
./mvnw test

# 執行特定測試類別
./mvnw test -Dtest=TemplateControllerTest

# 執行整合測試
./mvnw verify

# 產生測試覆蓋率報告
./mvnw jacoco:report
```

### 模板目錄設定（開發必須）
```bash
# 建立模板目錄
sudo mkdir -p /data/bpm-elf-backend/templates

# 複製範例模板供開發使用
cp -r src/main/resources/templates/* /data/bpm-elf-backend/templates/
```

### 健康檢查與 API 測試
```bash
# 應用程式健康檢查
curl http://localhost:8080/actuator/health

# 測試主要 API 端點
curl http://localhost:8080/bpm-elf/api/settings
curl http://localhost:8080/bpm-elf/api/sidebar
```

## 架構概述

### 核心系統架構
這是一個 Spring Boot 應用程式，作為業務流程管理（BPM）的模板處理引擎。系統將具有特殊語法的 JSON 模板轉換為 IT 服務申請的動態表單。

**核心處理流程：**
1. 原始 JSON 模板儲存在 `/data/bpm-elf-backend/templates/` 目錄
2. 模板可使用 `$include` 語法引用其他模板
3. 模板可使用 `$enum` 語法引用預定義的組件定義
4. TicketProcessorService 透過多個階段處理這些模板：
   - **mergeJsonReferences()**：解析 `$include` 引用（最多 2 層深度）
   - **processEnums()**：將 `$enum` 替換為實際組件定義
   - **aggregateTicketsColumn()**：將重複欄位合併到「基本資料」區塊

### 模板語法系統
應用程式實作自訂 JSON 模板系統，具有兩個主要語法擴充：

**$include 語法** - 模板組合：
```json
{
  "$include": "common-fields.json"
}
```

**$enum 語法** - 組件引用：
```json
{
  "$enum": "Dropdown.DEPARTMENT"
}
```

EnumRegistry 作為組件枚舉的中央查找，模板支援無限巢狀結構並具有循環引用保護機制。

### 檔案系統整合
- 模板**不會**打包到 WAR 檔案中（在 pom.xml 中排除）
- 模板必須在執行時存在於 `/data/bpm-elf-backend/templates/` 目錄
- 系統支援一般模板和子目錄中的「組合」模板
- 模板驗證防止目錄遍歷攻擊，並限制檔案類型為 `.json`

### 動態側邊欄生成
系統根據模板中的 `path` 屬性自動生成階層式導航：
```json
{
  "name": "模板名稱",
  "path": ["IT01_資訊需求單", "帳號管理", "系統名稱"],
  "inputs": [...]
}
```

## 核心組件

### TicketProcessorService
處理所有模板處理的核心業務邏輯服務：
- **模板聚合**：跨票據合併重複輸入欄位
- **引用解析**：處理 `$include` 和 `$enum` 語法
- **安全驗證**：防止路徑遍歷並驗證檔案類型
- **側邊欄生成**：從模板路徑建立動態導航

### TemplateController
提供 REST API 控制器：
- `/bpm-elf/api/settings` - 處理過的模板
- `/bpm-elf/api/settings-raw` - 原始模板內容
- `/bpm-elf/api/setting/{filename}` - 個別模板 CRUD 操作
- `/bpm-elf/api/sidebar` - 動態導航
- `/bpm-elf/api/prerenderData` - 模板變數替換

### ComponentEnum 系統
`constant` 套件中基於註冊表的組件系統：
- **ComponentEnum**：所有組件類型的介面
- **EnumRegistry**：枚舉組件的中央查找
- **Dropdown/Radio/Text**：特定組件實作

## 開發注意事項

### 模板開發
建立或修改模板時：
1. 模板必須是有效的 JSON，副檔名為 `.json`
2. 使用 `$include` 來重複使用通用欄位
3. 使用 `$enum` 來引用標準化組件（定義在 constant 套件中）
4. `path` 陣列決定側邊欄導航結構
5. 變數替換在描述欄位中使用 `${變數名稱}` 語法

### 自訂建置流程
Maven 建置包含自訂步驟：
1. **版本檔案生成**：建立包含專案版本的 `version.txt`
2. **模擬環境設定**：建立模擬部署的 `mockEnv/` 目錄結構
3. **Docker 映像檔建立**：建置後端和前端映像檔
4. **模板複製**：將模板複製到模擬環境（不打包到 WAR 中）

### 配置注意事項
- 模板目錄可透過 application.yml 中的 `file.base.directory` 配置
- 應用程式預期 HOSTNAME 環境變數用於部署路徑
- 已啟用 Actuator 端點（health、info、metrics）供監控使用
- CORS 設定為接受所有來源（`@CrossOrigin(origins = "*")`）

### 安全性考量
- 檔案操作包含路徑遍歷保護（`filename.contains("..")`）
- 模板操作僅允許 `.json` 檔案
- 模板目錄應具有適當的檔案系統權限
- 未實作身份驗證/授權 - 生產環境應新增此功能