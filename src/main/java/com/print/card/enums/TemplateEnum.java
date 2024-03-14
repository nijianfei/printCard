package com.print.card.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TemplateEnum {
    T4("4"),
    T5("5")
            ;

    public static final Map<String, TemplateEnum> codeMap = Arrays.stream(TemplateEnum.values()).collect(Collectors.toMap(TemplateEnum::getCode, Function.identity()));
    private final String code;

    TemplateEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static boolean isExist(String code){
        return Objects.nonNull(codeMap.get(code));
    }
}
