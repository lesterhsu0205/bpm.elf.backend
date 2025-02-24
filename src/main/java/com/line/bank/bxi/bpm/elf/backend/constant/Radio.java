package com.line.bank.bxi.bpm.elf.backend.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum Radio implements ComponentEnum {
    ENV("env", "環境", "radio", Arrays.asList("DEV", "STG", "UAT", "PRD")),
    DB_TOOL("db_tool", "DB","radio", Arrays.asList("MySQL", "Microsoft SQL Server", "PostgreSQL", "Oracle SQL Developer"));

    private final String key;
    private final String label;
    private final String type;
    private final List<String> options;

    Radio(String key, String label, String type, List<String> options) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.options = options;
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
                "key", key,
                "options", options.stream()
                        .map(option -> Map.of("text", option))
                        .collect(Collectors.toList())
        );
    }
}
