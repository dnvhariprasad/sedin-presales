package com.sedin.presales.application.dto.templateconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfig {

    private String version;
    private String aspectRatio;
    private double slideWidth;
    private double slideHeight;
    private BrandingConfig branding;
    private List<SectionConfig> sections;
    private String backgroundImage;
    private String footerText;
}
