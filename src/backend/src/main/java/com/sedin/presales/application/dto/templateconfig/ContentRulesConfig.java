package com.sedin.presales.application.dto.templateconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentRulesConfig {

    private Integer maxCharacters;
    private Integer minBullets;
    private Integer maxBullets;
    private Integer maxBulletChars;
    private Integer minItems;
    private Integer maxItems;
}
