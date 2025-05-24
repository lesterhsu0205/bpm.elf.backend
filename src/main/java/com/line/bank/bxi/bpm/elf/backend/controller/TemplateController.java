package com.line.bank.bxi.bpm.elf.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.line.bank.bxi.bpm.elf.backend.service.TicketProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @Autowired
    private TicketProcessorService ticketProcessorService;

    @GetMapping("/settings-raw")
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

                            JsonNode jsonNode = ticketProcessorService.readJson(file);

                            Map<String, Object> jsonFile = new HashMap<>();
                            jsonFile.put("file", fileName);
                            jsonFile.put("isCompose", "compose".equals(file.getParent().getFileName().toString()));
                            jsonFile.put("content", jsonNode);  // 儲存為 JSON 結構
                            resultList.add(jsonFile);
                        } catch (IOException e) {
                            throw new RuntimeException("Error reading file: " + file.getFileName(), e);
                        }
                    });

            // 排序：isCompose = true 的排在最後
            resultList.sort(Comparator.comparing(map -> Boolean.TRUE.equals(map.get("isCompose"))));

            return ResponseEntity.ok(resultList);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Collections.singletonList(Map.of("error", "Unable to read templates directory: " + baseDirectory)));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<List<Map<String, Object>>> readAllTemplates() {
        try {

            return ResponseEntity.ok(ticketProcessorService.readAllTemplates());

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Collections.singletonList(Map.of("error", "Unable to read templates directory: " + baseDirectory)));
        }
    }

    @GetMapping(value = "/setting/compose/{filename}", produces = "application/json")
    public ResponseEntity<String> getComposeTemplate(@PathVariable("filename") String filename) {
        return ticketProcessorService.getTemplate(filename, true);
    }

    @GetMapping(value = "/setting/{filename}", produces = "application/json")
    public ResponseEntity<String> getTemplate(@PathVariable("filename") String filename) {
        return ticketProcessorService.getTemplate(filename, false);
    }

    @GetMapping(value = "/sidebar", produces = "application/json")
    public ResponseEntity<String> getSideBar() {
        return ticketProcessorService.getSideBar();
    }

    @PostMapping("/setting/{filename}")
    public ResponseEntity<Map<String, Object>> writeTemplate(@PathVariable("filename") String filename, @RequestBody String jsonContent) {
        Map<String, Object> response = new HashMap<>();

        try {
            ticketProcessorService.validFileExtention(filename);

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
            return ResponseEntity.status(500).body(Map.of("error", "Error writing file: " + e.getMessage()));
        }
    }


    @DeleteMapping(value = "/setting/{filename}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable("filename") String filename) {
        Map<String, Object> response = new HashMap<>();

        try {
            ticketProcessorService.validFileExtention(filename);

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
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error processing file: " + e.getMessage()));
        }
    }
}
