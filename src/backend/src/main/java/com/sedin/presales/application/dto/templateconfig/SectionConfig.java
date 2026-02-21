package com.sedin.presales.application.dto.templateconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionConfig {

    private String key;
    private String label;
    private boolean required;
    private int order;
    private SectionType type;
    private PositionConfig position;
    private ContentRulesConfig contentRules;

    public enum SectionType {
        TEXT,
        BULLET_LIST,
        TAG_LIST,
        IMAGE
    }
}
