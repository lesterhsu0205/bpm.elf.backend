package com.line.bank.bxi.bpm.elf.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.line.bank.bxi.bpm.elf.backend.constant.ComponentEnum;
import com.line.bank.bxi.bpm.elf.backend.constant.EnumRegistry;
import com.line.bank.bxi.bpm.elf.backend.model.TicketInputRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * TicketProcessorService: 處理 JSON 中的 tickets 陣列，合併重複欄位到 "基本資料" 票據
 */
@Service
public class TicketProcessorService {
    private static final ObjectMapper mapper = new ObjectMapper();

    // 從 application.yml 中讀取設定的 BASE_DIRECTORY
    @Value("${file.base.directory}")
    private String baseDirectory;

    public JsonNode aggregateTicketsColumn(JsonNode root) {
        if (!root.has("tickets") || !root.get("tickets").isArray()) {
            return root;
        }
        ArrayNode tickets = (ArrayNode) root.get("tickets");

        // 尋找或建立 "基本資料" 票據
        ObjectNode basicTicket = null;
        for (JsonNode t : tickets) {
            if (t.has("name") && "基本資料".equals(t.get("name").asText())) {
                basicTicket = (ObjectNode) t;
                break;
            }
        }
        if (basicTicket == null) {
            basicTicket = mapper.createObjectNode();
            basicTicket.put("name", "基本資料");
            basicTicket.set("inputs", mapper.createArrayNode());
            tickets.insert(0, basicTicket);
        }
        ArrayNode basicInputs = (ArrayNode) basicTicket.get("inputs");

        // 用於去除重複欄位的 Set，存放已新增 input 的 key
        Set<String> addedKeys = new HashSet<>();
        basicInputs.forEach(node -> {
            if (node.has("key")) {
                addedKeys.add(node.get("key").asText());
            }
        });

        // 收集非基本票據中的 input key 出現位置
        Map<String, List<TicketInputRef>> occurrences = new HashMap<>();
        for (int i = 0; i < tickets.size(); i++) {
            JsonNode t = tickets.get(i);
            if (t == basicTicket) continue;
            ArrayNode inputs = (ArrayNode) t.get("inputs");
            for (int j = 0; j < inputs.size(); j++) {
                JsonNode input = inputs.get(j);
                if (input.has("key")) {
                    String key = input.get("key").asText();
                    occurrences.computeIfAbsent(key, k -> new ArrayList<>()).add(new TicketInputRef(t.get("name").asText()));
                }
            }
        }

        // 對於出現次數 > 1 的 key，執行移除並加入基本票據
        for (Map.Entry<String, List<TicketInputRef>> entry : occurrences.entrySet()) {
            List<TicketInputRef> refs = entry.getValue();
            if (refs.size() > 1) {

                JsonNode removed = null;

                for (TicketInputRef ref : refs) {

                    for (JsonNode ticket : tickets) {
                        if (ticket.has("name") && ref.getTicketName().equalsIgnoreCase(ticket.get("name").asText())) {
                            ArrayNode inputs = (ArrayNode) ticket.get("inputs");

                            for (int j = 0; j < inputs.size(); j++) {
                                JsonNode input = inputs.get(j);
                                if (input.has("key") && entry.getKey().equalsIgnoreCase(input.get("key").asText())) {
                                    removed = inputs.remove(j);
                                }
                            }
                            // 不可能有同名 ticket, 處理完第一個就可跳出
                            break;
                        }
                    }
                }

                // 只有當 key 未添加過時才加入
                String key = removed.has("key") ? removed.get("key").asText() : null;
                if (key != null && !addedKeys.contains(key)) {
                    basicInputs.add(removed);
                    addedKeys.add(key);
                }
            }
        }

        if (basicInputs.isEmpty()) {
            tickets.remove(0);
        }

        return root;
    }

    public void validFileExtention(String filename) {
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
    public JsonNode readJson(Path path) throws IOException {
        String jsonContent = Files.readString(path);
        return mapper.readTree(jsonContent);
    }

    public JsonNode processEnums(JsonNode root) {
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
                        JsonNode enumJson = mapper.valueToTree(componentEnum.toJson());
                        return enumJson; // 直接替換當前節點
                    } catch (Exception e) {
                        // 找不到時，保留原始 JSON
//                        System.out.println(e);
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
    public JsonNode mergeJsonReferences(String fileName, JsonNode node, Set<String> processedRefs, int depth) {

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
            ArrayNode arrayNode = mapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(mergeJsonReferences(fileName, element, new HashSet<>(processedRefs), depth));
            }
            return arrayNode;
        }

        return node;
    }

    public ResponseEntity<String> getTemplate(String filename, boolean isCompose) {
        try {
            validFileExtention(filename);

            String jsonContent;

            // 正式環境：直接從文件系統讀取
            String filePath = isCompose ? baseDirectory + "/compose/" + filename : baseDirectory + "/" + filename;
            JsonNode jsonNode = readJson(Paths.get(filePath));

            jsonContent = getJsonContent(filename, jsonNode);

            return ResponseEntity.ok().body(jsonContent);

        } catch (IOException e) {
            return ResponseEntity.status(404).body("Error reading file: " + filename + ", cause: " + e);
        }
    }

    public ResponseEntity<String> getSideBar() {
        try {

            ArrayNode sidebarNode = mapper.createArrayNode();

            List<Map<String, Object>> allSettings = readAllTemplates();

            ObjectNode composeNode = mapper.createObjectNode();
            ArrayNode childrenNode = mapper.createArrayNode();

            composeNode.put("name", "Compose 需求單");
            composeNode.put("icon", "ticket");
            composeNode.set("children", childrenNode);

            for (Map<String, Object> setting : allSettings) {
                if (setting.containsKey("isCompose") && (boolean) setting.get("isCompose")) {
                    String filename = setting.get("file").toString();
                    JsonNode contentNode = mapper.readTree(setting.get("content").toString());
                    ObjectNode child = mapper.createObjectNode();
                    child.put("name", contentNode.get("name").textValue());
                    child.put("icon", "ticket");
                    child.put("url", "/compose/" + filename.replace(".json", ""));
                    childrenNode.add(child);
                } else {

                    ObjectNode contentNode = (ObjectNode) setting.get("content");

                    if (!contentNode.has("path")) {
                        continue;
                    }

                    ArrayNode pathsNode = (ArrayNode) contentNode.get("path");
                    String[] pathArray = mapper.convertValue(pathsNode, String[].class);
                    assignItemNode(sidebarNode, pathArray, setting);
                }
            }

            sidebarNode.add(composeNode);

            String jsonContent = mapper.writeValueAsString(sidebarNode);
            return ResponseEntity.ok().body(jsonContent);
        } catch (IOException e) {
            return ResponseEntity.status(404).body("Error reading file: sidebar.json, cause: " + e);
        }
    }

    public List<Map<String, Object>> readAllTemplates() throws IOException {

        List<Map<String, Object>> resultList = new ArrayList<>();

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
                        jsonFile.put("isCompose", "compose".equals(file.getParent().getFileName().toString()));
                        jsonFile.put("content", jsonNode);  // 儲存為 JSON 結構
                        resultList.add(jsonFile);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file: " + file.getFileName(), e);
                    }
                });

        // 排序：isCompose = true 的排在最後
        resultList.sort(Comparator.comparing(map -> Boolean.TRUE.equals(map.get("isCompose"))));
        return resultList;
    }

    public String getJsonContent(String filename, JsonNode jsonNode) throws JsonProcessingException {
        String jsonContent;
        Set<String> processedRefs = new HashSet<>();  // 追蹤當前請求內的 JSON 檔案
        int initialDepth = 0; // 記錄當前遞迴深度

        jsonNode = mergeJsonReferences(filename, jsonNode, processedRefs, initialDepth);
        jsonNode = processEnums(jsonNode);
        removeRecursive(null, null, jsonNode, "$enum", "$include");
        jsonNode = aggregateTicketsColumn(jsonNode);
        jsonContent = mapper.writeValueAsString(jsonNode);
        return jsonContent;
    }

    private void assignItemNode(JsonNode parenNode, String[] existPaths, Map<String, Object> setting) {

        if (existPaths.length > 0) {

            // 查詢有無已建立過相同 path 的 itemNode
            boolean isExistItemnode = false;

            for (JsonNode node : parenNode) {
                if (node.has("name") && existPaths[0].equals(node.get("name").textValue())) {
                    isExistItemnode = true;

                    ObjectNode itemNode = (ObjectNode) node;

                    ArrayNode childrenNode = null;
                    if (itemNode.has("children")) {
                        childrenNode = (ArrayNode) itemNode.get("children");
                    } else {
                        childrenNode = mapper.createArrayNode();
                        itemNode.set("children", childrenNode);
                    }

//                    if (existPaths.length > 1) {
//                        ObjectNode subItemNode = mapper.createObjectNode();
//
//                        subItemNode.put("name", existPaths[1]);
//                        subItemNode.put("icon", "ticket");
//
//                        childrenNode.add(subItemNode);
//                    }

                    String[] newExistPaths = Arrays.copyOfRange(existPaths, 1, existPaths.length);

                    assignItemNode(childrenNode, newExistPaths, setting);
                    break;
                }
            }

            // 沒建立就直接建立
            if (!isExistItemnode) {
                ObjectNode itemNode = mapper.createObjectNode();
                ArrayNode childrenNode = mapper.createArrayNode();

                itemNode.put("name", existPaths[0]);
                itemNode.put("icon", "ticket");
                itemNode.put("children", childrenNode);

                ArrayNode parentArrayNode = (ArrayNode) parenNode;
                parentArrayNode.add(itemNode);
                sortArrayNodeByName(parentArrayNode);

                String[] newExistPaths = Arrays.copyOfRange(existPaths, 1, existPaths.length);
                assignItemNode(childrenNode, newExistPaths, setting);
            }

        } else {
            ObjectNode itemNode = mapper.createObjectNode();

            ObjectNode contentNode = (ObjectNode) setting.get("content");

            itemNode.put("name", contentNode.get("name").textValue());
            itemNode.put("icon", "ticket");

            String[] pathArray = mapper.convertValue(contentNode.get("path"), String[].class);

            List<String> pathList = new ArrayList<>(Arrays.stream(pathArray).toList());
            pathList.add(setting.get("file").toString().replace(".json", ""));

            itemNode.put("url", pathList.stream().collect(Collectors.joining("/", "/", "")));

            ArrayNode parentArrayNode = (ArrayNode) parenNode;
            parentArrayNode.add(itemNode);
            sortArrayNodeByName(parentArrayNode);
        }

    }

    private void sortArrayNodeByName(ArrayNode arrayNode) {

        // 1. 準備 Collator
        Collator collator = Collator.getInstance(Locale.TRADITIONAL_CHINESE);
        collator.setStrength(Collator.PRIMARY);

        // 2. 轉為 List<JsonNode>
        List<JsonNode> list = StreamSupport.stream(arrayNode.spliterator(), false).collect(Collectors.toList());

        // 3. 依 name 排序
        list.sort((a, b) -> collator.compare(a.get("name").asText(), b.get("name").asText()));

        // 4. 清空並回填
        arrayNode.removeAll();
        list.forEach(arrayNode::add);
    }

    private void removeRecursive(ObjectNode parent, String keyInParent, JsonNode node, String... fieldNames) {
        if (node == null) return;

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            // 遞迴處理所有子欄位
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            List<String> keysToRemove = new ArrayList<>();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode childNode = entry.getValue();

                // 遞迴處理子節點
                removeRecursive(objectNode, fieldName, childNode, fieldNames);
            }

            // 刪除欄位名稱符合的
            for (String fieldName : fieldNames) {
                objectNode.remove(fieldName);
            }

            // 檢查是否為空 → 如果是，就請父層刪除它
            if (objectNode.size() == 0 && parent != null && keyInParent != null) {
                parent.remove(keyInParent);
            }

        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;

            // 反向遍歷，避免刪除時 index 亂掉
            for (int i = arrayNode.size() - 1; i >= 0; i--) {
                JsonNode element = arrayNode.get(i);
                removeRecursive(null, null, element, fieldNames);

                // 若 element 被處理後變成空物件或空陣列，也刪掉它
                if ((element.isObject() && element.size() == 0) ||
                        (element.isArray() && element.size() == 0)) {
                    arrayNode.remove(i);
                }
            }

            // 整個陣列變空，也刪除它
            if (arrayNode.size() == 0 && parent != null && keyInParent != null) {
                parent.remove(keyInParent);
            }
        }
    }
}