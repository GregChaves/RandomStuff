package com.chaves.pdfTransfer;

import java.io.*;
import java.net.MalformedURLException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
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
import org.springframework.web.multipart.commons.CommonsMultipartFile;
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

    public String zipFiles(@RequestParam List<String> name, String action) throws IOException {

        FileOutputStream fos = null;
        ZipOutputStream zipOut = null;
        FileInputStream fis = null;

        String zipFolderName = "";


        if (action.equalsIgnoreCase("extraction")){
            zipFolderName ="boletos_unificados" + ".zip";

        }else if(action.equalsIgnoreCase("rename")){
            zipFolderName ="boletos_renomeados" + ".zip";
        }

        String filepathzipped = zipFolderName;

        fos = new FileOutputStream(filepathzipped);

        zipOut = new ZipOutputStream(new BufferedOutputStream(fos));

        Resource resourceZip = null;

        for (String fileName : name) {

            System.out.println("zipando o arquivo: " + fileName);

            Path path = Paths.get(fileName);
            Resource resource = new UrlResource(path.toUri().toURL());
            ZipEntry zipEntry = new ZipEntry(resource.getFilename());

            zipEntry.setSize(resource.contentLength());
            try {
                zipOut.putNextEntry(zipEntry);
            }

            catch (IOException e) {
            zipOut.putNextEntry(new ZipEntry(resource.getFilename().concat("XW")));
            System.out.println("Numero do doc repetido?");

            }

            StreamUtils.copy(resource.getInputStream(), zipOut);
            zipOut.closeEntry();
        }

        zipOut.finish();
        zipOut.close();

        return filepathzipped;

    }

    @PostMapping("/upload")
    public List<String> uploadPdfAndGenerateNewFiles(@RequestParam("file") MultipartFile multipartFile) throws IOException {

        System.out.println("Entering in uploadPdfAndGenerateNewFiles method");

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

                outputDoc.save(content[7].split(" ")[0]+".pdf");

                String fileName = new String(content[7].split(" ")[0] + ".pdf");

                fileNames.add(content[7].split(" ")[0] + ".pdf");

                System.out.println("Gerando o arquivo fileName pdf: " + fileName);

                outputDoc.close();

                index++;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        doc.close();

        System.out.println("chamando metodo para zipar os arquivos");

        String zipUrl = zipFiles(fileNames, "extraction");

        fileNames.add(zipUrl);

        return fileNames;


    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity downloadFile(@PathVariable String fileName) {

        System.out.println("entrando no metodo downloadFile recebendo o arquivo: " + fileName);

        Path path = Paths.get(fileName);
        Resource resource = null;
        try {
            resource = new UrlResource(path.toUri().toURL());

            System.out.println("resource: " + resource.getFilename());
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

        System.out.println("entrando no metodo downloadZipFile recebendo o arquivo: " + zipName);

        Path path = Paths.get(zipName);
        Resource resource = null;
        try {
            resource = new UrlResource(path.toUri().toURL());
            System.out.println("resource: " + resource.getFilename());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload/zipfile")
    public List<String> uploadZipAndRenameFiles(@RequestParam("file") MultipartFile files) throws IOException {

        System.out.println("entrando no metodo uploadZipAndRenameFiles recebendo o arquivo: " + files.getOriginalFilename());

        unzipFile(files);

        List<String> finalNames = new ArrayList<String>();

        List<File> filesInFolder = Files.walk(Paths.get("unzipTest"))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        for (File filePdf: filesInFolder) {

            FileItem fileItem = new DiskFileItemFactory().createItem("filex",
                    Files.probeContentType(filePdf.toPath()), false, filePdf.getName());

            try (InputStream in = new FileInputStream(filePdf); OutputStream out = fileItem.getOutputStream()) {
                in.transferTo(out);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid file: " + e, e);
            }

            System.out.println("##### "+filePdf.getName());

            if (!filePdf.getName().contains(".pdf")){
                continue;

            }

            CommonsMultipartFile multipartFile = new CommonsMultipartFile(fileItem);

            PdfReader pdfReader = new PdfReader(multipartFile.getInputStream());

            String pageContent = PdfTextExtractor.getTextFromPage(pdfReader, 1);
            pageContent = pageContent.trim();

            String content[] = pageContent.split("\n");
            String novoNomedoArquivo = (content[4].split(" ")[0]+".pdf");

            //Files.deleteIfExists(filePdf.toPath());
            //Files.move(filePdf.toPath(), filePdf.toPath().resolveSibling(novoNomedoArquivo));
            filePdf.renameTo(new File(novoNomedoArquivo));

            finalNames.add(novoNomedoArquivo);

            System.out.println(filePdf.getName());

        }

        String zipUrl = zipFiles(finalNames, "rename");

        finalNames.add(zipUrl);

        return finalNames;

    }

    public void unzipFile(MultipartFile fileZip) throws IOException {

        File destDir = new File("unzipTest");
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream((fileZip.getInputStream()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    @GetMapping("/status")
    public String serviceStatus() {
        return "Running!";
    }

}
