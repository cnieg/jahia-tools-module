package fr.cnieg.jahia.tools.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

@UtilityClass
public class ZipUtil {

    public void unzip(final Path path, final Charset charset) throws IOException {
        String fileBaseName = FilenameUtils.getBaseName(path.getFileName().toString());
        Path destFolderPath = Paths.get(path.getParent().toString(), fileBaseName);

        try (ZipFile zipFile = new ZipFile(path.toFile(), ZipFile.OPEN_READ, charset)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destFolderPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())) {
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        }
    }
}
