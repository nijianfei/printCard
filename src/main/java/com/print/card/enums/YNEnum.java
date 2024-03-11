package com.print.card.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum YNEnum {
    YES("0"),
    NO("1")
    ;

    public static final Map<String, YNEnum> codeMap = Arrays.stream(YNEnum.values()).collect(Collectors.toMap(YNEnum::getCode, Function.identity()));
    private final String code;

    YNEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static boolean isExist(String code){
        return Objects.nonNull(codeMap.get(code));
    }
}
