package com.print.card.dto;


import java.util.Map;


public class TemplateConfig {

    public TemplateConfig() {
    }

    public TemplateConfig(Map<String, Template> templateMap) {
        this.templateMap = templateMap;
    }

    private Map<String, Template> templateMap;

    public Map<String, Template> getTemplateMap() {
        return templateMap;
    }

    public void setTemplateMap(Map<String, Template> templateMap) {
        this.templateMap = templateMap;
    }
}