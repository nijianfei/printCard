package com.print.card.utils;



import java.util.Map;

public class LabelUtil {
    private static final String LABLE_START = "<";
    private static final String LABLE_END = ">";
    private static final String VIRGULE_SLASH = "/";
    private static final String SPACE = "&nbsp";
    private static final String NEWLINE = "<br>";
    private static final String NEWLINE_SPACE_LABEL = "<br>&nbsp;";
    private final static String DOUBLE_QUOTES = "\"";

    public static String buildLabel(String labelName, String content) {
        return buildLabel(labelName, content, null);
    }

    public static String buildLabel(String labelName, String content, Map<String, String> attribute) {
        StringBuilder lableTemp = new StringBuilder();
        lableTemp.append(LABLE_START);
        lableTemp.append(labelName);
        lableTemp.append(buildAttribute(attribute));
        lableTemp.append(LABLE_END);
        lableTemp.append(content);
        lableTemp.append(LABLE_START);
        lableTemp.append(VIRGULE_SLASH);
        lableTemp.append(labelName);
        lableTemp.append(LABLE_END);
        return lableTemp.toString();
    }

    public static String buildAttribute(Map<String, String> attribute) {
        if (attribute == null || attribute.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        attribute.forEach((k, v) -> sb.append(" ").append(k).append("=").append(StringUtil.wrapWithSymbols(v, DOUBLE_QUOTES)));
        return sb.toString();
    }

}
