package com.sedin.presales.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Slf4j
@Configuration
public class AsposeConfig {

    private static final String LICENSE_PATH = "license/Aspose.Total.Java.lic";

    @PostConstruct
    public void loadAsposeLicenses() {
        InputStream licenseStream = getClass().getClassLoader().getResourceAsStream(LICENSE_PATH);

        if (licenseStream == null) {
            log.warn("Aspose license file not found at '{}'. Running in evaluation mode.", LICENSE_PATH);
            return;
        }

        loadSlidesLicense();
        loadWordsLicense();
        loadCellsLicense();
    }

    private void loadSlidesLicense() {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(LICENSE_PATH);
            if (stream != null) {
                com.aspose.slides.License slidesLicense = new com.aspose.slides.License();
                slidesLicense.setLicense(stream);
                log.info("Aspose.Slides license loaded successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to load Aspose.Slides license: {}. Running in evaluation mode.", e.getMessage());
        }
    }

    private void loadWordsLicense() {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(LICENSE_PATH);
            if (stream != null) {
                com.aspose.words.License wordsLicense = new com.aspose.words.License();
                wordsLicense.setLicense(stream);
                log.info("Aspose.Words license loaded successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to load Aspose.Words license: {}. Running in evaluation mode.", e.getMessage());
        }
    }

    private void loadCellsLicense() {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(LICENSE_PATH);
            if (stream != null) {
                com.aspose.cells.License cellsLicense = new com.aspose.cells.License();
                cellsLicense.setLicense(stream);
                log.info("Aspose.Cells license loaded successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to load Aspose.Cells license: {}. Running in evaluation mode.", e.getMessage());
        }
    }
}
