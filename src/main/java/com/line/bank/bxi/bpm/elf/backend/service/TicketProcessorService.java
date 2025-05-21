package com.line.bank.bxi.bpm.elf.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.line.bank.bxi.bpm.elf.backend.model.TicketInputRef;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TicketProcessorService: 處理 JSON 中的 tickets 陣列，合併重複欄位到 "基本資料" 票據
 */
@Service
public class TicketProcessorService {
    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonNode process(JsonNode root) {
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
//            tickets.insert(0, basicTicket);
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
                    occurrences.computeIfAbsent(key, k -> new LinkedList<>())
                            .add(new TicketInputRef(i, j));
                }
            }
        }

        // 對於出現次數 > 1 的 key，執行移除並加入基本票據
        for (Map.Entry<String, List<TicketInputRef>> entry : occurrences.entrySet()) {
            List<TicketInputRef> refs = entry.getValue();
            if (refs.size() > 1) {
                // 先從後往前刪除，以免索引錯亂
                for (TicketInputRef ref : refs) {
                    ObjectNode ticketNode = (ObjectNode) tickets.get(ref.getTicketIndex());
                    ArrayNode inputs = (ArrayNode) ticketNode.get("inputs");
                    JsonNode removed = inputs.remove(ref.getInputIndex());
                    // 只有當 key 未添加過時才加入
                    String key = removed.has("key") ? removed.get("key").asText() : null;
                    if (key != null && !addedKeys.contains(key)) {
                        basicInputs.add(removed);
                        addedKeys.add(key);
                    }
                }
            }
        }

        if (!basicInputs.isEmpty()) {
            tickets.insert(0, basicTicket);
        }

        return root;
    }
}