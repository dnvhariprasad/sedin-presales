package com.sedin.presales.infrastructure.rendition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PptTextExtractorTest {

    @InjectMocks
    private PptTextExtractor pptTextExtractor;

    @Test
    void extractText_shouldThrowWhenStreamIsNull() {
        assertThatThrownBy(() -> pptTextExtractor.extractText(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to extract text from PPT presentation");
    }
}
