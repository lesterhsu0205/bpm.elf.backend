package com.line.bank.bxi.bpm.elf.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.line.bank.bxi.bpm.elf.backend.constant.ComponentEnum;
import com.line.bank.bxi.bpm.elf.backend.constant.EnumRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/bpm-elf/api")
@CrossOrigin(origins = "*")
public class TemplateController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 從 application.yml 中讀取設定的 BASE_DIRECTORY
    @Value("${file.base.directory}")
    private String baseDirectory;

    @GetMapping("/read-settings-raw")
    public ResponseEntity<List<Map<String, Object>>> readAllRawTemplates() {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            // 獲取 resources/templates 目錄內的所有 JSON 檔案
            Path templatesPath = Paths.get(baseDirectory);

            // 遍歷 JSON 檔案
            Files.walk(templatesPath).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json"))  // 只處理 .json 檔案
                    .forEach(file -> {
                        try {
                            String fileName = file.getFileName().toString();

                            JsonNode jsonNode = readJson(file);

                            Map<String, Object> jsonFile = new HashMap<>();
                            jsonFile.put("file", fileName);
                            jsonFile.put("content", jsonNode);  // 儲存為 JSON 結構
                            resultList.add(jsonFile);
                        } catch (IOException e) {
                            throw new RuntimeException("Error reading file: " + file.getFileName(), e);
                        }
                    });

            return ResponseEntity.ok(resultList);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Collections.singletonList(Map.of("error", "Unable to read templates directory")));
        }
    }

    @GetMapping("/read-settings")
    public ResponseEntity<List<Map<String, Object>>> readAllTemplates() {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            // 獲取 resources/templates 目錄內的所有 JSON 檔案
            Path templatesPath = Paths.get(baseDirectory);

            // 遍歷 JSON 檔案
            Files.walk(templatesPath).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json"))  // 只處理 .json 檔案
                    .forEach(file -> {
                        try {
                            String fileName = file.getFileName().toString();

                            JsonNode jsonNode = readJson(file);

                            Set<String> processedRefs = new HashSet<>();  // 追蹤當前請求內的 JSON 檔案
                            int initialDepth = 0; // 記錄當前遞迴深度

                            jsonNode = mergeJsonReferences(fileName, jsonNode, processedRefs, initialDepth);
                            jsonNode = processEnums(jsonNode);

                            Map<String, Object> jsonFile = new HashMap<>();
                            jsonFile.put("file", fileName);
                            jsonFile.put("content", jsonNode);  // 儲存為 JSON 結構
                            resultList.add(jsonFile);
                        } catch (IOException e) {
                            throw new RuntimeException("Error reading file: " + file.getFileName(), e);
                        }
                    });

            return ResponseEntity.ok(resultList);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Collections.singletonList(Map.of("error", "Unable to read templates directory")));
        }
    }

    @GetMapping(value = "/read-setting/{filename}", produces = "application/json")
    public ResponseEntity<String> getTemplate(@PathVariable("filename") String filename) {
        try {
            validFileExtention(filename);

            String jsonContent;

            // 檢查是否使用 classpath (開發環境)
            if (baseDirectory.startsWith("classpath:")) {
                String classpathPath = baseDirectory.replace("classpath:", "");
                Resource resource = new ClassPathResource(classpathPath + filename);
                if (!resource.exists()) {
                    return ResponseEntity.status(404).body("File not found: " + filename);
                }
                jsonContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } else {
                // 正式環境：直接從文件系統讀取
                String filePath = baseDirectory + filename;
                JsonNode jsonNode = readJson(Paths.get(filePath));

                Set<String> processedRefs = new HashSet<>();  // 追蹤當前請求內的 JSON 檔案
                int initialDepth = 0; // 記錄當前遞迴深度

                jsonNode = mergeJsonReferences(filename, jsonNode, processedRefs, initialDepth);
                jsonNode = processEnums(jsonNode);
                jsonContent = objectMapper.writeValueAsString(jsonNode);
//                jsonContent = Files.readString(Paths.get(filePath));
            }

            return ResponseEntity.ok().body(jsonContent);

        } catch (IOException e) {
            return ResponseEntity.status(404).body("Error reading file: " + filename);
        }
    }


    @PostMapping("/write-setting/{filename}")
    public ResponseEntity<Map<String, Object>> writeTemplate(@PathVariable("filename") String filename, @RequestBody String jsonContent) {
        Map<String, Object> response = new HashMap<>();

        try {
            validFileExtention(filename);

            // 組合檔案的絕對路徑
            Path filePath = Paths.get(baseDirectory, filename);

            // 確保目錄存在，若不存在則建立
            Files.createDirectories(filePath.getParent());

            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            String lfContent = prettyJson.replace("\r\n", "\n");

            // 寫入 JSON 內容（覆蓋或新增）
            Files.writeString(filePath, lfContent);

            response.put("message", "File written successfully: " + filename);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("error", "Error writing file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    @DeleteMapping(value = "/delete-setting/{filename}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable("filename") String filename) {
        Map<String, Object> response = new HashMap<>();

        try {
            validFileExtention(filename);

            if (baseDirectory.startsWith("classpath:")) {
                // 專案 resources 目錄內的檔案位置 (直接存取專案內的檔案)
                Path resourcePath = Paths.get(baseDirectory, filename);

                if (Files.notExists(resourcePath)) {
                    response.put("error", "File not found in development environment: " + filename);
                    return ResponseEntity.status(404).body(response);
                }

                boolean deleted = Files.deleteIfExists(resourcePath);

                if (deleted) {
                    response.put("message", "File deleted successfully in development: " + filename);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("error", "Failed to delete file in development: " + filename);
                    return ResponseEntity.status(500).body(response);
                }
            } else {
                // 正式環境：從指定的目錄刪除
                Path filePath = Paths.get(baseDirectory, filename);

                if (Files.notExists(filePath)) {
                    response.put("error", "File not found: " + filename);
                    return ResponseEntity.status(404).body(response);
                }

                boolean deleted = Files.deleteIfExists(filePath);

                if (deleted) {
                    response.put("message", "File deleted successfully: " + filename);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("error", "Failed to delete file: " + filename);
                    return ResponseEntity.status(500).body(response);
                }
            }
        } catch (IOException e) {
            response.put("error", "Error processing file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    private void validFileExtention(String filename) {
        // 確保檔案名為 JSON，避免不安全的請求
        if (!filename.endsWith(".json")) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid file type, only .json files are allowed.");
            ResponseEntity.badRequest().body(response);
        }

        // 確保路徑安全，避免目錄遍歷攻擊
        if (filename.contains("..")) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid file name.");
            ResponseEntity.badRequest().body(response);
        }
    }

    // 讀取 JSON 檔案
    private JsonNode readJson(Path path) throws IOException {
        String jsonContent = Files.readString(path);
        return objectMapper.readTree(jsonContent);
    }

    private JsonNode processEnums(JsonNode root) {
        if (root.isObject()) {
            ObjectNode objectNode = (ObjectNode) root;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if ("$enum".equals(key) && value.isTextual()) {
                    String enumFullName = value.asText(); // e.g. "Dropdown.DEPARTMENT"

                    try {
                        String[] parts = enumFullName.split("\\.");
                        if (parts.length != 2) {
                            throw new IllegalArgumentException("Invalid $enum format: " + enumFullName);
                        }
                        String enumName = parts[1]; // e.g. "DEPARTMENT"

                        // 透過 EnumRegistry 快速查找
                        ComponentEnum componentEnum = EnumRegistry.getByEnumName(enumName);
                        if (componentEnum == null) {
                            return root; // 找不到時不處理，直接回傳原始 JSON
                        }

                        // 取得 Enum JSON
                        JsonNode enumJson = objectMapper.valueToTree(componentEnum.toJson());
                        return enumJson; // 直接替換當前節點
                    } catch (Exception e) {
                        // 找不到時，保留原始 JSON
                        System.out.println(e);
                    }
                } else {
                    objectNode.set(key, processEnums(value));
                }
            }
        } else if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, processEnums(arrayNode.get(i))); // 正確處理 ArrayNode
            }
        }
        return root;
    }

    // 遞歸合併 JSON 內的 "$include"
    private JsonNode mergeJsonReferences(String fileName, JsonNode node, Set<String> processedRefs, int depth) {

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            // 遞歸處理所有子節點
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                if ("$include".equalsIgnoreCase(fieldName)) {
                    String refFileName = objectNode.get("$include").asText();

                    // 避免自己 include 自己
                    if (refFileName.equalsIgnoreCase(fileName)) {
                        return objectNode;
                    }

                    // 避免無窮遞迴：當前這次遞迴內已經解析過該 JSON，則直接返回，不展開
                    if (processedRefs.contains(refFileName)) {
                        return objectNode;
                    }

                    // 限制最多展開兩層，超過兩層的 "ref" 保留原始內容
                    if (depth >= 1) {
                        return objectNode;
                    }

                    // 讀取副 JSON 並合併
                    try {
                        JsonNode refJson = readJson(Paths.get(baseDirectory, refFileName));

                        // 標記此 JSON 檔案已經被展開（僅限於這一輪處理）
                        processedRefs.add(refFileName);

                        // 進一步展開 refJson
                        return mergeJsonReferences(fileName, refJson, new HashSet<>(processedRefs), depth + 1);
                    } catch (Exception e) {
                        objectNode.put(fieldName, e.toString());
//                        return objectNode;
                    }
                } else {
                    objectNode.set(fieldName, mergeJsonReferences(fileName, objectNode.get(fieldName), new HashSet<>(processedRefs), depth));
                }
            }
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(mergeJsonReferences(fileName, element, new HashSet<>(processedRefs), depth));
            }
            return arrayNode;
        }

        return node;

    }
}
