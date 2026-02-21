package com.sedin.presales.infrastructure.rendition;

import com.aspose.slides.IAutoShape;
import com.aspose.slides.IShape;
import com.aspose.slides.ISlide;
import com.aspose.slides.Presentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Slf4j
@Service
public class PptTextExtractor {

    public String extractText(InputStream pptStream) {
        log.info("Extracting text from PPT presentation");
        try {
            Presentation presentation = new Presentation(pptStream);
            try {
                StringBuilder sb = new StringBuilder();
                for (ISlide slide : presentation.getSlides()) {
                    for (IShape shape : slide.getShapes()) {
                        if (shape instanceof IAutoShape autoShape) {
                            if (autoShape.getTextFrame() != null) {
                                sb.append(autoShape.getTextFrame().getText()).append("\n");
                            }
                        }
                    }
                    sb.append("\n---\n"); // slide separator
                }
                String text = sb.toString().trim();
                log.info("Extracted {} characters from PPT", text.length());
                return text;
            } finally {
                presentation.dispose();
            }
        } catch (Exception e) {
            log.error("Failed to extract text from PPT", e);
            throw new RuntimeException("Failed to extract text from PPT presentation", e);
        }
    }
}
