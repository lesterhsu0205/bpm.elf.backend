package com.line.bank.bxi.bpm.elf.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // 從 application.yml 中讀取設定的 BASE_DIRECTORY
    @Value("${file.base.directory}")
    private String baseDirectory;

    @GetMapping("/read-settings")
    public ResponseEntity<List<Map<String, Object>>> readAllTemplates() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 獲取 resources/templates 目錄內的所有 JSON 檔案
            Path templatesPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", baseDirectory.replace("classpath:", ""));

            // 遍歷 JSON 檔案
            Files.walk(templatesPath).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json"))  // 只處理 .json 檔案
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file);  // 讀取 JSON 檔案內容
                            JsonNode jsonNode = objectMapper.readTree(content); // 解析為 JSON 物件
                            Map<String, Object> jsonFile = new HashMap<>();
                            jsonFile.put("file", file.getFileName().toString());
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
                jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
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
            Path filePath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", baseDirectory.replace("classpath:", ""), filename);

            // 確保目錄存在，若不存在則建立
            Files.createDirectories(filePath.getParent());

            ObjectMapper objectMapper = new ObjectMapper();

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
                Path resourcePath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", baseDirectory.replace("classpath:", ""), filename);

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

}
