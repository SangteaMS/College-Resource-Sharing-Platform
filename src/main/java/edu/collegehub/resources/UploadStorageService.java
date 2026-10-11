package edu.collegehub.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadStorageService {
    private final Path root;
    private final long maxSize;

    public UploadStorageService(@Value("${collegehub.storage.location:./uploads}") String location,
                                @Value("${collegehub.upload.max-size-bytes:20971520}") long maxSize) {
        this.root = Path.of(location).toAbsolutePath().normalize();
        this.maxSize = maxSize;
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a file to upload.");
        if (file.getSize() > maxSize) throw new IllegalArgumentException("The file is larger than the configured upload limit of " + (maxSize / 1_048_576) + " MB.");
        String displayName = sanitizeFilename(file.getOriginalFilename());
        String extension = extension(displayName);
        FileFormat format = formatForExtension(extension);
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) throw new IllegalArgumentException("The selected file is empty.");
            validateContent(bytes, format);
            return new ValidatedUpload(displayName, format, bytes);
        } catch (IOException ex) {
            throw new IllegalArgumentException("The selected file could not be read. Please choose it again.");
        }
    }

    public StoredUpload store(ValidatedUpload upload) {
        String generated = UUID.randomUUID() + "." + upload.format().getExtension();
        Path target = safePath(generated);
        try {
            Files.createDirectories(root);
            Files.write(target, upload.bytes());
            cleanupIfTransactionRollsBack(target);
            return new StoredUpload(generated, upload.displayFilename(), upload.format(), upload.bytes().length, target);
        } catch (IOException ex) {
            throw new IllegalStateException("The file could not be saved. Please try again.", ex);
        }
    }

    public StoredUpload storePdf(String name, String content) {
        byte[] bytes = makeSimplePdf(content);
        return store(new ValidatedUpload(sanitizeFilename(name + ".pdf"), FileFormat.PDF, bytes));
    }
    public StoredUpload storeDemoScript(String name, String content) {
        String safeName = sanitizeFilename(name + ".R");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String generated = UUID.randomUUID() + ".r";
        Path target = safePath(generated);
        try {
            Files.createDirectories(root);
            Files.write(target, bytes);
            cleanupIfTransactionRollsBack(target);
            return new StoredUpload(generated, safeName, FileFormat.R_SCRIPT, bytes.length, target);
        } catch (IOException ex) { throw new IllegalStateException("The sample script could not be saved.", ex); }
    }

    public Path pathFor(String storedFilename) {
        if (storedFilename == null || !storedFilename.matches("[0-9a-fA-F-]{36}\\.[a-z]{1,4}")) {
            throw new IllegalArgumentException("Invalid stored file name.");
        }
        return safePath(storedFilename);
    }

    public void deleteAfterCommit(Path path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteQuietly(path); }
            });
        } else deleteQuietly(path);
    }

    private void cleanupIfTransactionRollsBack(Path path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) deleteQuietly(path);
                }
            });
        }
    }
    private Path safePath(String filename) {
        Path resolved = root.resolve(filename).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid file path.");
        return resolved;
    }
    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }
    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) throw new IllegalArgumentException("Choose a file with a name.");
        String base = original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        base = base.replaceAll("[^\\p{L}\\p{N}._ -]", "_").replaceAll("^\\.+", "").trim();
        if (base.isBlank() || base.length() > 255) throw new IllegalArgumentException("The file name is not valid or is too long.");
        return base;
    }
    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || dot == filename.length() - 1) throw new IllegalArgumentException("Use a PDF, PPTX, DOCX, JPG, or PNG file.");
        return filename.substring(dot + 1).toLowerCase();
    }
    private FileFormat formatForExtension(String extension) {
        return switch (extension) {
            case "pdf" -> FileFormat.PDF;
            case "pptx" -> FileFormat.PPTX;
            case "docx" -> FileFormat.DOCX;
            case "jpg", "jpeg" -> FileFormat.JPG;
            case "png" -> FileFormat.PNG;
            default -> throw new IllegalArgumentException("Use a PDF, PPTX, DOCX, JPG, or PNG file.");
        };
    }
    private void validateContent(byte[] bytes, FileFormat format) {
        boolean valid = switch (format) {
            case PDF -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case JPG -> bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case PNG -> startsWith(bytes, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            case DOCX -> isOfficeZip(bytes, "word/document.xml");
            case PPTX -> isOfficeZip(bytes, "ppt/presentation.xml");
            case R_SCRIPT -> false;
        };
        if (!valid) throw new IllegalArgumentException("The file contents do not match the selected file format.");
    }
    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
        return true;
    }
    private boolean isOfficeZip(byte[] bytes, String requiredFile) {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') return false;
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) entries.add(entry.getName());
            return entries.contains("[Content_Types].xml") && entries.contains(requiredFile);
        } catch (IOException ex) { return false; }
    }

    private byte[] makeSimplePdf(String text) {
        String printable = text.replace("\r", "").replaceAll("[^\\x20-\\x7E\\n\\t]", " ");
        ArrayList<String> lines = new ArrayList<>();
        for (String paragraph : printable.split("\\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                if (word.isBlank()) continue;
                if (!line.isEmpty() && line.length() + word.length() + 1 > 82) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) lines.add(line.toString());
            else lines.add("");
        }
        StringBuilder streamContent = new StringBuilder("BT /F1 11 Tf 54 740 Td ");
        for (int i = 0; i < Math.min(lines.size(), 42); i++) {
            if (i > 0) streamContent.append("0 -16 Td ");
            String safeLine = lines.get(i).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            streamContent.append('(').append(safeLine).append(") Tj ");
        }
        streamContent.append("ET");
        String stream = streamContent.toString();
        String[] objects = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + stream + "\nendstream"
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
            pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.length; i++) pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        pdf.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    public record ValidatedUpload(String displayFilename, FileFormat format, byte[] bytes) { }
    public record StoredUpload(String storedFilename, String displayFilename, FileFormat format, long size, Path path) { }
}
