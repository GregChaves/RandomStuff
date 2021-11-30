package com.chaves.pdfTransfer;

import java.io.*;
import java.net.MalformedURLException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;


import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;




@RestController
@RequestMapping("/api/v1/pdf")
public class PdfTransferController {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS").allowedOrigins("*");
            }
        };
    }

    public String zipFiles(@RequestParam List<String> name) throws IOException {

        FileOutputStream fos = null;
        ZipOutputStream zipOut = null;
        FileInputStream fis = null;

        String zipFolderName ="boletos_unificados" + ".zip";

        String filepathzipped = "files\\download\\" +zipFolderName;

        fos = new FileOutputStream(filepathzipped);

        zipOut = new ZipOutputStream(new BufferedOutputStream(fos));

        Resource resourceZip = null;

        for (String fileName : name) {
            Path path = Paths.get(fileName);
            Resource resource = new UrlResource(path.toUri().toURL());
            ZipEntry zipEntry = new ZipEntry(resource.getFilename());
            zipEntry.setSize(resource.contentLength());
            zipOut.putNextEntry(zipEntry);
            StreamUtils.copy(resource.getInputStream(), zipOut);
            zipOut.closeEntry();
        }

        zipOut.finish();
        zipOut.close();

        return filepathzipped;

    }

    @PostMapping("/upload")
    public List<String> uploadPdfAndGenerateNewFiles(@RequestParam("file") MultipartFile multipartFile) throws IOException {

        PdfReader pdfReader = new PdfReader(multipartFile.getInputStream());

        PDDocument doc = PDDocument.load(multipartFile.getInputStream());

        List<String> fileNames = new ArrayList<>();

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

                outputDoc.save("files\\download\\"+content[7].split(" ")[0]+".pdf");

                String fileName = new String("files\\download\\"+content[7].split(" ")[0] + ".pdf");

                fileNames.add(fileName);

                outputDoc.close();

                index++;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        doc.close();

        String zipUrl = zipFiles(fileNames);

        fileNames.add(zipUrl);

        return fileNames;


    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity downloadFileFromLocal(@PathVariable String fileName) {
        Path path = Paths.get("files\\download\\" + fileName);
        Resource resource = null;
        try {
            resource = new UrlResource(path.toUri().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/pdf"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/downloadzip/{zipName}")
    public ResponseEntity downloadZipFile(@PathVariable String zipName) {
        Path path = Paths.get("files\\download\\" + zipName);
        Resource resource = null;
        try {
            resource = new UrlResource(path.toUri().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/status")
    public String serviceStatus() {
        return "Running!";
    }

}