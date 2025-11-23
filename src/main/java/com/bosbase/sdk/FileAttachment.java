package com.bosbase.sdk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import okhttp3.MediaType;

/**
 * Simple wrapper for multipart file uploads.
 */
public class FileAttachment {
    public final String filename;
    public final byte[] bytes;
    public final String contentType;
    public final MediaType mediaType;

    public FileAttachment(String filename, byte[] bytes, String contentType) {
        this.filename = filename;
        this.bytes = bytes;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.mediaType = MediaType.parse(this.contentType);
    }

    public static FileAttachment fromFile(File file) throws IOException {
        return fromFile(file, null);
    }

    public static FileAttachment fromFile(File file, String contentType) throws IOException {
        String detected = contentType != null
            ? contentType
            : Files.probeContentType(file.toPath());
        if (detected == null || detected.isBlank()) {
            detected = "application/octet-stream";
        }
        return new FileAttachment(file.getName(), Files.readAllBytes(file.toPath()), detected);
    }
}
