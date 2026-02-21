package com.sedin.presales.application.dto.templateconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrandingConfig {

    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String fontFamily;
    private String headingFontFamily;
    private int headingFontSize;
    private int bodyFontSize;
    private int bulletFontSize;
}
