package com.line.bank.bxi.bpm.elf.backend.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum Dropdown implements ComponentEnum {
    ENV("env", "環境", "radio", Arrays.asList("DEV", "STG", "UAT", "PRD")),
    DEPARTMENT("department", "部門", "select", Arrays.asList("系統整合應用部", "營運支援系統部", "台幣存匯系統部", "台幣共用系統部", "支付系統部", "風險系統部", "台幣放款系統部", "保險系統部", "外匯暨信託系統部", "QA暨資訊營管部")),
    DIVISION("division", "處別","select", Arrays.asList("核心系統處", "業務系統處"));

    private final String key;
    private final String label;
    private final String type;
    private final List<String> options;

    Dropdown(String key, String label, String type, List<String> options) {
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
