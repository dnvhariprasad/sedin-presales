package com.sedin.presales.infrastructure.rendition;

import com.aspose.cells.PdfSaveOptions;
import com.aspose.cells.Workbook;
import com.aspose.slides.Presentation;
import com.aspose.words.Document;
import com.sedin.presales.application.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
@Service
public class PdfRenditionService {

    public byte[] convertToPdf(InputStream fileStream, String contentType) {
        log.info("Converting file with content type '{}' to PDF", contentType);

        if (contentType == null) {
            throw new BadRequestException("Content type is required for PDF conversion");
        }

        return switch (contentType) {
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                 "application/vnd.ms-powerpoint" -> convertPresentationToPdf(fileStream);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/msword" -> convertWordToPdf(fileStream);
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.ms-excel" -> convertSpreadsheetToPdf(fileStream);
            case "application/pdf" -> readAllBytes(fileStream);
            default -> throw new BadRequestException("Unsupported content type for PDF conversion: " + contentType);
        };
    }

    private byte[] convertPresentationToPdf(InputStream fileStream) {
        log.debug("Converting presentation to PDF using Aspose.Slides");
        try {
            Presentation presentation = new Presentation(fileStream);
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                presentation.save(outputStream, com.aspose.slides.SaveFormat.Pdf);
                log.debug("Presentation converted to PDF successfully, size: {} bytes", outputStream.size());
                return outputStream.toByteArray();
            } finally {
                presentation.dispose();
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert presentation to PDF", e);
            throw new BadRequestException("Failed to convert presentation to PDF");
        }
    }

    private byte[] convertWordToPdf(InputStream fileStream) {
        log.debug("Converting Word document to PDF using Aspose.Words");
        try {
            Document document = new Document(fileStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream, com.aspose.words.SaveFormat.PDF);
            log.debug("Word document converted to PDF successfully, size: {} bytes", outputStream.size());
            return outputStream.toByteArray();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert Word document to PDF", e);
            throw new BadRequestException("Failed to convert Word document to PDF");
        }
    }

    private byte[] convertSpreadsheetToPdf(InputStream fileStream) {
        log.debug("Converting spreadsheet to PDF using Aspose.Cells");
        try {
            Workbook workbook = new Workbook(fileStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfSaveOptions saveOptions = new PdfSaveOptions();
            workbook.save(outputStream, saveOptions);
            log.debug("Spreadsheet converted to PDF successfully, size: {} bytes", outputStream.size());
            return outputStream.toByteArray();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert spreadsheet to PDF", e);
            throw new BadRequestException("Failed to convert spreadsheet to PDF");
        }
    }

    private byte[] readAllBytes(InputStream fileStream) {
        log.debug("File is already PDF, reading bytes");
        try {
            return fileStream.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to read PDF file", e);
            throw new BadRequestException("Failed to read PDF file");
        }
    }
}
