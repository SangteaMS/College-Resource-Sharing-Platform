package edu.collegehub.resources;

public enum FileFormat {
    PDF("pdf", "PDF", "application/pdf"), PPTX("pptx", "PPTX", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    DOCX("docx", "DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    JPG("jpg", "JPG", "image/jpeg"), PNG("png", "PNG", "image/png"), R_SCRIPT("r", "R script", "text/plain");
    private final String extension;
    private final String label;
    private final String mediaType;
    FileFormat(String extension, String label, String mediaType) { this.extension = extension; this.label = label; this.mediaType = mediaType; }
    public String getExtension() { return extension; }
    public String getLabel() { return label; }
    public String getMediaType() { return mediaType; }
}
