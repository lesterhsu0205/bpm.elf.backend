package com.line.bank.bxi.bpm.elf.backend.constant;

import java.util.Map;

public interface ComponentEnum {

    String getKey(); // 取得 Key

    Map<String, Object> toJson(); // 轉成 JSON

    // 讓 `DropdownEnum` 也有 `name()` 方法
    default String name() {
        return ((Enum<?>) this).name();
    }
}
