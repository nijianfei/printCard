package com.print.card.utils;


import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.stream.Collectors;

public class StringUtil {

    /**
     * 使用默认字符（"'"）对内容包裹
     *
     * @param sourceStr
     * @return
     */
    public static String wrapWithSymbols(String sourceStr) {
        return wrapWithSymbols(sourceStr, "'");
    }

    /**
     * 用指定字符对内容包裹
     *
     * @param sourceStr
     * @param specifyChar
     * @return
     */
    public static String wrapWithSymbols(String sourceStr, String specifyChar) {
        return new StringBuilder().append(specifyChar).append(sourceStr).append(specifyChar).toString();
    }

    /**
     * @param paramList
     * @return
     */
    public static String wrapWithSymbols(Collection<String> paramList) {
        return CollectionUtils.isEmpty(paramList) ? null : paramList.stream().map(StringUtil::wrapWithSymbols).collect(Collectors.joining(","));
    }


}
