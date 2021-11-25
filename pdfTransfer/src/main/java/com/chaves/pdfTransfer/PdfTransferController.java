package com.chaves.pdfTransfer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.commons.fileupload.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/api/v1/pdf")
public class PdfTransferController {

    @PostMapping("/upload")
    public ResponseEntity registerStudentForCourse(@RequestParam("file") MultipartFile multipartFile) throws IOException {

        File file = FileUtils.getFile(multipartFile.getOriginalFilename());

        //PdfReader pdfReader = new PdfReader("C:\\Users\\gbritoch\\Downloads\\Borderô 5 Salt.pdf");

        PdfReader pdfReader = new PdfReader(multipartFile.getInputStream());

        PDDocument doc = PDDocument.load(multipartFile.getInputStream());

        try {

            int index = 1;
            for (PDPage originalPage : doc.getPages()) {

                PDDocument outputDoc = new PDDocument();
                outputDoc = new PDDocument();
                outputDoc.getDocument().setVersion(doc.getDocument().getVersion());
                outputDoc.setDocumentInformation(doc.getDocumentInformation());
                outputDoc.getDocumentCatalog().setViewerPreferences(doc.getDocumentCatalog().getViewerPreferences());

                PDFCloneUtility cloner = new PDFCloneUtility(outputDoc);

                COSDictionary pageDictionary = (COSDictionary) cloner.cloneForNewDocument(originalPage);
                PDPage page = new PDPage(pageDictionary);
                outputDoc.addPage(page);

                String pageContent = PdfTextExtractor.getTextFromPage(pdfReader, index);
                pageContent = pageContent.trim();
                String content[] = pageContent.split("\n");

                outputDoc.save(content[7].split(" ")[0]+".pdf");
                outputDoc.close();
                index++;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        doc.close();

        String fileDownloadUri = "ok";


        return ResponseEntity.ok(fileDownloadUri);
    }

    private static void addText(PDDocument document, PDPage page, String myText, float x, float y) {

        try {
            // Get Content Stream for Writing Data
            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

            // Begin the Content stream
            contentStream.beginText();

            // Setting the font to the Content stream
            contentStream.setFont(PDType1Font.COURIER, 12);


            // Setting the position for the line
            contentStream.newLineAtOffset(x, y);

            // Adding text in the form of string
            contentStream.showText(myText);

            // Ending the content stream
            contentStream.endText();

            // Closing the content stream
            contentStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



        @GetMapping("/status")
    public String serviceStatus() {
        return "Running!";
    }



   /* @GetMapping(value = "/upload-pdf", produces="application/zip")
    public void zipDownload(@RequestParam("file") MultipartFile file) throws IOException {


        ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
        for (String fileName : name) {
            FileSystemResource resource = new FileSystemResource("/" + fileName);
            ZipEntry zipEntry = new ZipEntry(resource.getFilename());
            zipEntry.setSize(resource.contentLength());
            zipOut.putNextEntry(zipEntry);
            StreamUtils.copy(resource.getInputStream(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
        zipOut.close();
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipOut + "\"");
    }*/


}