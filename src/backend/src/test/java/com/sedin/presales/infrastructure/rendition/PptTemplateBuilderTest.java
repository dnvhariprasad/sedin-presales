package com.sedin.presales.infrastructure.rendition;

import com.sedin.presales.application.dto.templateconfig.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PptTemplateBuilderTest {

    @InjectMocks
    private PptTemplateBuilder pptTemplateBuilder;

    private TemplateConfig buildMinimalConfig() {
        PositionConfig position = new PositionConfig(0.5, 0.5, 12.0, 1.0);

        BrandingConfig branding = new BrandingConfig();
        branding.setPrimaryColor("#003366");
        branding.setFontFamily("Arial");
        branding.setHeadingFontFamily("Arial");
        branding.setHeadingFontSize(24);
        branding.setBodyFontSize(14);
        branding.setBulletFontSize(12);

        SectionConfig section = new SectionConfig();
        section.setKey("title");
        section.setLabel("Title");
        section.setRequired(true);
        section.setOrder(1);
        section.setType(SectionConfig.SectionType.TEXT);
        section.setPosition(position);

        TemplateConfig config = new TemplateConfig();
        config.setVersion("1.0");
        config.setSlideWidth(13.33);
        config.setSlideHeight(7.5);
        config.setBranding(branding);
        config.setSections(List.of(section));
        return config;
    }

    @Test
    void buildPresentation_shouldReturnNonEmptyBytes() {
        TemplateConfig config = buildMinimalConfig();
        Map<String, Object> contentMap = Map.of("title", "Test Case Study");

        byte[] result = pptTemplateBuilder.buildPresentation(config, contentMap);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    void buildPresentation_shouldHandleEmptyContentMap() {
        TemplateConfig config = buildMinimalConfig();
        Map<String, Object> contentMap = Map.of();

        byte[] result = pptTemplateBuilder.buildPresentation(config, contentMap);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }
}
