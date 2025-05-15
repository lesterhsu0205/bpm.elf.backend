package com.line.bank.bxi.bpm.elf.backend.constant;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnumRegistry {

    private static final Map<String, ComponentEnum> ENUM_MAP = new HashMap<>();

    static {
        // 註冊所有 Enum 到 MAP
        registerEnum(Dropdown.values());
        registerEnum(Text.values());
        registerEnum(Radio.values());
    }

    private static void registerEnum(ComponentEnum[] values) {
        ENUM_MAP.putAll(Arrays.stream(values)
                .collect(Collectors.toMap(ComponentEnum::name, Function.identity())));
    }

    // 透過 enumName 快速查找，找不到時直接回傳 null
    public static ComponentEnum getByEnumName(String enumName) {
        return ENUM_MAP.get(enumName); // O(1) 查找，不遍歷
    }
}
