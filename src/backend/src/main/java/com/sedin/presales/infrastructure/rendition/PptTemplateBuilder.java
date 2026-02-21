package com.sedin.presales.infrastructure.rendition;

import com.aspose.slides.*;
import com.sedin.presales.application.dto.templateconfig.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PptTemplateBuilder {

    public byte[] buildPresentation(TemplateConfig config, Map<String, Object> contentMap) {
        log.info("Building PPT presentation from template config");
        Presentation presentation = new Presentation();
        try {
            // Set slide size (inches to points: 1 inch = 72 points)
            presentation.getSlideSize().setSize(
                    (float) (config.getSlideWidth() * 72),
                    (float) (config.getSlideHeight() * 72),
                    SlideSizeScaleType.DoNotScale
            );

            // Use the first (default) slide
            ISlide slide = presentation.getSlides().get_Item(0);

            // Apply branding background
            if (config.getBranding() != null && config.getBranding().getPrimaryColor() != null) {
                slide.getBackground().setType(BackgroundType.OwnBackground);
                slide.getBackground().getFillFormat().setFillType(FillType.Solid);
                slide.getBackground().getFillFormat().getSolidFillColor().setColor(Color.WHITE);
            }

            // Add sections in order
            if (config.getSections() != null) {
                List<SectionConfig> sortedSections = config.getSections().stream()
                        .sorted(Comparator.comparingInt(SectionConfig::getOrder))
                        .toList();

                for (SectionConfig section : sortedSections) {
                    Object content = contentMap.get(section.getKey());
                    if (content == null) continue;

                    addSectionToSlide(slide, section, content, config.getBranding());
                }
            }

            // Add footer
            if (config.getFooterText() != null) {
                addFooter(slide, config);
            }

            // Save to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            presentation.save(outputStream, SaveFormat.Pptx);
            byte[] result = outputStream.toByteArray();
            log.info("PPT presentation built successfully, size: {} bytes", result.length);
            return result;

        } catch (Exception e) {
            log.error("Failed to build PPT presentation", e);
            throw new RuntimeException("Failed to build PPT presentation", e);
        } finally {
            presentation.dispose();
        }
    }

    private void addSectionToSlide(ISlide slide, SectionConfig section, Object content, BrandingConfig branding) {
        PositionConfig pos = section.getPosition();
        if (pos == null) return;

        float x = (float) (pos.getX() * 72);
        float y = (float) (pos.getY() * 72);
        float width = (float) (pos.getWidth() * 72);
        float height = (float) (pos.getHeight() * 72);

        IAutoShape shape = slide.getShapes().addAutoShape(ShapeType.Rectangle, x, y, width, height);
        shape.getFillFormat().setFillType(FillType.NoFill);
        shape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        ITextFrame textFrame = shape.getTextFrame();
        textFrame.getParagraphs().clear();

        // Add section label as heading
        IParagraph labelPara = new Paragraph();
        IPortion labelPortion = new Portion(section.getLabel());
        if (branding != null) {
            labelPortion.getPortionFormat().setFontHeight(branding.getHeadingFontSize());
            labelPortion.getPortionFormat().setFontBold(NullableBool.True);
            if (branding.getPrimaryColor() != null) {
                labelPortion.getPortionFormat().getFillFormat().setFillType(FillType.Solid);
                labelPortion.getPortionFormat().getFillFormat().getSolidFillColor().setColor(parseColor(branding.getPrimaryColor()));
            }
            if (branding.getHeadingFontFamily() != null) {
                labelPortion.getPortionFormat().setLatinFont(new FontData(branding.getHeadingFontFamily()));
            }
        }
        labelPara.getPortions().add(labelPortion);
        textFrame.getParagraphs().add(labelPara);

        // Add content based on section type
        switch (section.getType()) {
            case TEXT -> addTextContent(textFrame, content.toString(), branding);
            case BULLET_LIST -> addBulletListContent(textFrame, content, branding);
            case TAG_LIST -> addTagListContent(textFrame, content, branding);
            default -> addTextContent(textFrame, content.toString(), branding);
        }
    }

    private void addTextContent(ITextFrame textFrame, String text, BrandingConfig branding) {
        IParagraph para = new Paragraph();
        IPortion portion = new Portion(text);
        if (branding != null) {
            portion.getPortionFormat().setFontHeight(branding.getBodyFontSize());
            if (branding.getFontFamily() != null) {
                portion.getPortionFormat().setLatinFont(new FontData(branding.getFontFamily()));
            }
        }
        para.getPortions().add(portion);
        textFrame.getParagraphs().add(para);
    }

    @SuppressWarnings("unchecked")
    private void addBulletListContent(ITextFrame textFrame, Object content, BrandingConfig branding) {
        List<String> items;
        if (content instanceof List<?> list) {
            items = (List<String>) list;
        } else {
            items = List.of(content.toString());
        }

        for (String item : items) {
            IParagraph para = new Paragraph();
            para.getParagraphFormat().getBullet().setType(BulletType.Symbol);
            para.getParagraphFormat().getBullet().setChar((char) 8226); // bullet character
            para.getParagraphFormat().setIndent(20);

            IPortion portion = new Portion(item);
            if (branding != null) {
                portion.getPortionFormat().setFontHeight(branding.getBulletFontSize());
                if (branding.getFontFamily() != null) {
                    portion.getPortionFormat().setLatinFont(new FontData(branding.getFontFamily()));
                }
            }
            para.getPortions().add(portion);
            textFrame.getParagraphs().add(para);
        }
    }

    @SuppressWarnings("unchecked")
    private void addTagListContent(ITextFrame textFrame, Object content, BrandingConfig branding) {
        List<String> tags;
        if (content instanceof List<?> list) {
            tags = (List<String>) list;
        } else {
            tags = List.of(content.toString());
        }

        IParagraph para = new Paragraph();
        String tagText = String.join("  |  ", tags);
        IPortion portion = new Portion(tagText);
        if (branding != null) {
            portion.getPortionFormat().setFontHeight(branding.getBodyFontSize());
            if (branding.getAccentColor() != null) {
                portion.getPortionFormat().getFillFormat().setFillType(FillType.Solid);
                portion.getPortionFormat().getFillFormat().getSolidFillColor().setColor(parseColor(branding.getAccentColor()));
            }
            if (branding.getFontFamily() != null) {
                portion.getPortionFormat().setLatinFont(new FontData(branding.getFontFamily()));
            }
        }
        para.getPortions().add(portion);
        textFrame.getParagraphs().add(para);
    }

    private void addFooter(ISlide slide, TemplateConfig config) {
        float slideWidth = (float) (config.getSlideWidth() * 72);
        float slideHeight = (float) (config.getSlideHeight() * 72);
        float footerHeight = 20;
        float footerY = slideHeight - footerHeight - 10;

        IAutoShape footerShape = slide.getShapes().addAutoShape(ShapeType.Rectangle, 36, footerY, slideWidth - 72, footerHeight);
        footerShape.getFillFormat().setFillType(FillType.NoFill);
        footerShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        ITextFrame textFrame = footerShape.getTextFrame();
        textFrame.getParagraphs().clear();

        IParagraph para = new Paragraph();
        para.getParagraphFormat().setAlignment(TextAlignment.Center);
        IPortion portion = new Portion(config.getFooterText());
        portion.getPortionFormat().setFontHeight(8);
        portion.getPortionFormat().getFillFormat().setFillType(FillType.Solid);
        portion.getPortionFormat().getFillFormat().getSolidFillColor().setColor(Color.GRAY);
        para.getPortions().add(portion);
        textFrame.getParagraphs().add(para);
    }

    private Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }
}
