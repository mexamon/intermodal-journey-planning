package com.thy.cloud.base.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaFileAggregator {

    private static final String OUTPUT_FILE = "output.txt";

    public static void main(String[] args) {
        try {
            aggregateJavaFiles();
            System.out.println("output.txt dosyası başarıyla oluşturuldu.");
        } catch (IOException | URISyntaxException e) {
            System.err.println("Dosya oluşturma sırasında hata oluştu: " + e.getMessage());
        }
    }

    private static void aggregateJavaFiles() throws IOException, URISyntaxException {
        Path currentDir = Paths.get(JavaFileAggregator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path projectRoot = currentDir;
        while (projectRoot != null && !Files.exists(projectRoot.resolve("src"))) {
            projectRoot = projectRoot.getParent();
        }

        if (projectRoot == null) {
            throw new IOException("src dizini bulunamadı.");
        }

        String packagePath = JavaFileAggregator.class.getPackageName().replace('.', '/');
        Path srcMainJavaDir = projectRoot.resolve("src/main/java").resolve(packagePath);
        Path outputPath = projectRoot.resolve(OUTPUT_FILE);

        try (Stream<Path> pathStream = Files.walk(srcMainJavaDir)) {
            List<Path> javaFiles = pathStream
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            StringBuilder aggregatedContent = new StringBuilder();

            for (Path javaFile : javaFiles) {
                aggregatedContent.append("// File-Name: ").append(javaFile.getFileName()).append("\n");
                List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
                for (String line : lines) {
                    aggregatedContent.append(line).append("\n");
                }
                aggregatedContent.append("\n"); // Dosyalar arası boşluk ekle
            }

            Files.write(outputPath, aggregatedContent.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
