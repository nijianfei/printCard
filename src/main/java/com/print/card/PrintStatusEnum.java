package com.print.card;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PrintStatusEnum {
    Ready("Ready","就绪"),
    Busy("Busy","忙碌"),
    Error("Error","错误"),
    Offline("Offline","离线"),
    ErrorOtherDevice("ErrorOtherDevice","设备异常"),
    ;

    private static Map<String, PrintStatusEnum> cache = Arrays.stream(values()).collect(Collectors.toMap(o -> o.getCode(), Function.identity()));
    private String code;
    private String name;

    PrintStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PrintStatusEnum getByCode(String code){
        PrintStatusEnum printStatusEnum = cache.get(code);
        return printStatusEnum != null ?printStatusEnum : Error;
    }
}
