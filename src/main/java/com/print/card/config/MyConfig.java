package com.print.card.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.print.card.dto.Template;
import com.print.card.dto.TemplateConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;


@Configuration
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
public class MyConfig {
    @Value("${template.config}")
    private String templateStr;

    @Bean
    public TemplateConfig templateConfig() {
        TemplateConfig templateConfig = new TemplateConfig();
        Map<String, Template> tMap = new HashMap<>();
        JSONObject jsonObject = JSONObject.parseObject(templateStr);
        jsonObject.keySet().forEach(key->tMap.put(key, JSONObject.parseObject(JSON.toJSONString(jsonObject.get(key)),Template.class)));
        templateConfig.setTemplateMap(tMap);
        return templateConfig;
    }
}
