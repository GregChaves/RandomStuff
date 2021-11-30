package com.chaves.pdfTransfer;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.commons.fileupload.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.mock.web.MockMultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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

        //downloadZipFromLocal(zipFolderName);

        return filepathzipped;

    }

    @PostMapping("/upload")
    public List<String> uploadPdfAndGenerateNewFiles(@RequestParam("file") MultipartFile multipartFile) throws IOException {

        File file = FileUtils.getFile(multipartFile.getOriginalFilename());

        //PdfReader pdfReader = new PdfReader("C:\\Users\\gbritoch\\Downloads\\Borderô 5 Salt.pdf");

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
    public ResponseEntity downloadZipFromLocal(@PathVariable String zipName) {
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

    /*@GetMapping("/files")
    public List<String> getAllDownloadedFiles() {

        List<String> downloadFilesUrl = new ArrayList<>();

        Path path = Paths.
        Resource resource = null;
        try {
            resource = new UrlResource(path.toUri().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }*/




    @GetMapping("/status")
    public String serviceStatus() {
        return "Running!";
    }

}