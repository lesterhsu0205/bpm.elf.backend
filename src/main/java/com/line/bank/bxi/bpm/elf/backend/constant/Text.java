package com.line.bank.bxi.bpm.elf.backend.constant;

import java.util.Map;

public enum Text implements ComponentEnum {
    STAFF_ID("staffId", "員工編號"),
    ENG_NAME("engName", "英文名"),
    CHI_NAME("chiName", "中文名");

    private final String key;
    private final String label;
    private final String type;

    Text(String key, String label) {
        this.key = key;
        this.label = label;
        this.type = "text";
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Map<String, Object> toJson() {
        return Map.of(
                "type", type,
                "label", label,
                "key", key
        );
    }
}
