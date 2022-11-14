package com.wyverno;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileValidator {

    private final File ORIGINAL;
    private final File FOLDER;

    public FileValidator(File original, File folder) {
        this.ORIGINAL = original;
        this.FOLDER = folder;
    }

    public FileValidator(Path original, Path folder) {
        this.ORIGINAL = original.toFile();
        this.FOLDER = folder.toFile();
    }

    public void validateFiles() {
        System.out.println("Checking files by Names");
        this.compareAllFilesByName(this.ORIGINAL.toPath() ,this.FOLDER.toPath());
        System.out.println("Checking files by SHA256");
        this.compareAllFilesBySHA256(this.ORIGINAL.toPath(),this.FOLDER.toPath());
        System.out.println("Copy pasting files");
        this.copyPastingFiles(this.ORIGINAL.toPath(),this.FOLDER.toPath());
        System.out.println("FINISH!");
    }

    private void compareAllFilesByName(Path original, Path compareFolder) { // Сверяем файлы по имени и удаляем лишнии
        try {
            List<Path> compareFiles = Files.list(compareFolder)
                                           .collect(Collectors.toList());
            List<Path> originalFiles = Files.list(original)
                                            .collect(Collectors.toList());

            compareFiles.stream().filter(predicate -> // Удалям не совпавшиеся имена файлов
                    originalFiles.stream().noneMatch(path -> path.getFileName().equals(predicate.getFileName())))
                                 .forEach(this::deleteFile);

            originalFiles.stream().filter(Files::isDirectory) // Проверяем на наличие папок, если такие находим проверяем их внутриности.
                                  .forEach(originalFolder -> {
                                      compareFiles.stream()
                                                  .filter(p -> p.getFileName().equals(originalFolder.getFileName()))
                                                  .findAny()
                                                  .ifPresent(folder -> {
                                                      System.out.println("Checking in Folder by Name -> " + folder);
                                                      this.compareAllFilesByName(originalFolder, folder);
                                                  });});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void compareAllFilesBySHA256(Path original, Path compareFolder) { // Сверяем байты файла по хэш сумме и удаляем лишнии.
        try {

            List<Path> compareFiles = Files.list(compareFolder)
                                           .collect(Collectors.toList());

            List<Path> originalFiles = Files.list(original)
                                            .collect(Collectors.toList());

            compareFiles.stream()
                    .filter(cFile -> // Сверяем файлы по SHA256
                    {
                      if (Files.isDirectory(cFile)) return false;

                      return originalFiles.stream().noneMatch(oFile -> {
                         if (Files.isDirectory(oFile)) return false; // Если это папка то откидуем её.
                         try (FileInputStream cFileStream = new FileInputStream(cFile.toFile());
                              FileInputStream oFileStream = new FileInputStream(oFile.toFile())) {
                             String cFileSHA256 = DigestUtils.sha256Hex(cFileStream);
                             String oFileSHA256 = DigestUtils.sha256Hex(oFileStream);

                             return oFileSHA256.equals(cFileSHA256); // Если хэши одинаковы то окидуем файл, если хэши разные то оставляем в стриме.
                         } catch (IOException e) {
                             e.printStackTrace();
                         };
                         return false;
                      });
                    })
                    .forEach(this::deleteFile); // Удаляем все файлы которые были отфильтрованы.

            originalFiles.stream()
                    .filter(Files::isDirectory)
                    .forEach(oFolder -> {
                        compareFiles.stream()
                                .filter(Files::isDirectory)
                                .filter(cFolder -> oFolder.getFileName().equals(cFolder.getFileName()))
                                .findAny()
                                .ifPresent(cFolder -> {
                                    System.out.println("Checking in Folder by SHA256 -> " + cFolder);
                                    compareAllFilesBySHA256(oFolder,cFolder);
                                });
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyPastingFiles(Path original, Path compareFolder) {
        try {

            List<Path> compareFiles = Files.list(compareFolder)
                                           .collect(Collectors.toList());

            List<Path> originalFiles = Files.list(original)
                                            .collect(Collectors.toList());

            originalFiles.stream() // Переносим из оригинальной папки в статик
                    .filter(oFile -> compareFiles.stream().noneMatch(cFile -> oFile.getFileName().equals(cFile.getFileName())))
                    .forEach(oFile -> {
                        try {
                            System.out.println("Starting copying from " + oFile + " to folder " + compareFolder);
                            Files.copy(oFile, compareFolder.resolve(oFile.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
                            System.out.println("Finish copy file from " + oFile + " to folder " + compareFolder);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            originalFiles.stream() // Ищем все папки и в случае отсутвия в их файлов добавляем.
                    .filter(Files::isDirectory)
                    .forEach(originalFolder -> {
                        this.copyPastingFiles(originalFolder, compareFolder.resolve(originalFolder.getFileName()));
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFile(Path path) {
        try {
            if (Files.isDirectory(path) && !isEmptyFolder(path)) {
                Stream<Path> stream = Files.list(path);
                System.out.println("Delete content in Folder -> " + path);
                stream.forEach(this::deleteFile); // Удаляем содержимое папки.
                stream.close();
                System.out.println("Delete folder with content -> " + path);
                deleteFile(path); // Удаляем папку
            } else {
                Files.delete(path);
                System.out.println("Delete -> " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isEmptyFolder(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> e = Files.list(path)) {
                return !e.findFirst().isPresent();
            }
        }
        return false;
    }
}
