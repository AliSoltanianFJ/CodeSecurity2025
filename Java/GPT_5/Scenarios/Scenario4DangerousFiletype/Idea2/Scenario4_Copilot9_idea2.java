// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Secure PDF upload servlet.
 * - Validates multipart form
 * - Enforces max size
 * - Sanitizes filename
 * - Verifies PDF magic bytes
 * - Stores file under ./uploads with safe move
 */
public class Scenario4_Copilot9_idea2 {

    @MultipartConfig
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Max upload size: 10 MB
        private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
        private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]+");
        private static final int PDF_HEADER_LEN = 4;

        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain; charset=UTF-8");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Request must be multipart/form-data.");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file part named 'pdf' found.");
                return;
            }

            long size = filePart.getSize();
            if (size <= 0 || size > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File is empty or exceeds maximum allowed size (10 MB).");
                return;
            }

            String submittedName = sanitizeSubmittedFileName(filePart.getSubmittedFileName());
            // Ensure we have a reasonable filename; if not, generate a UUID-based name
            String filename = (isValidFilename(submittedName) && submittedName.toLowerCase().endsWith(".pdf"))
                    ? submittedName
                    : UUID.randomUUID().toString() + ".pdf";

            // Work in a temporary file first
            Path tempFile = null;
            try (InputStream in = filePart.getInputStream()) {
                tempFile = Files.createTempFile("upload-", ".tmp");
                // Copy stream to temp file (size already checked via Part.getSize)
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long copied = 0;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        copied += read;
                        if (copied > MAX_FILE_SIZE) { // double-check during copy
                            throw new IOException("Uploaded file exceeds allowed size.");
                        }
                    }
                }

                // Verify PDF magic bytes ("%PDF")
                try (InputStream tin = Files.newInputStream(tempFile)) {
                    byte[] header = new byte[PDF_HEADER_LEN];
                    int got = tin.read(header);
                    if (got != PDF_HEADER_LEN) {
                        throw new IOException("Uploaded file is too small or corrupted.");
                    }
                    String sig = new String(header, "ISO-8859-1");
                    if (!sig.startsWith("%PDF")) {
                        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                        resp.getWriter().write("Uploaded file is not a valid PDF.");
                        Files.deleteIfExists(tempFile);
                        return;
                    }
                }

                // Prepare uploads directory
                Path uploadsDir = Paths.get(".").toAbsolutePath().normalize().resolve("uploads").normalize();
                Files.createDirectories(uploadsDir);

                // Resolve target path and ensure no directory traversal escaped the uploads directory
                Path target = uploadsDir.resolve(filename).normalize();
                if (!target.startsWith(uploadsDir)) {
                    throw new IOException("Invalid file name or path.");
                }

                // Move temp file to final destination atomically if possible
                try {
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    tempFile = null; // moved successfully
                } catch (Exception ex) {
                    // fallback to non-atomic move
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                    tempFile = null;
                }

                // Try to set safe permissions on POSIX systems (ignored on Windows)
                try {
                    Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(target, perms);
                } catch (UnsupportedOperationException | IOException ignore) {
                    // Ignore: Windows or FS not supporting POSIX perms default perms will apply
                }

                // Success: avoid reflecting user supplied filename to prevent XSS
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("File uploaded successfully.");
            } catch (IOException e) {
                // Ensure temp file cleaned up on error
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignore) {}
                }
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to process uploaded file.");
                return;
            }
        }

        // Remove any path components and normalize nulls
        private static String sanitizeSubmittedFileName(String name) {
            if (name == null) return "";
            // Some browsers send full path, strip directories
            String simple = Paths.get(name).getFileName().toString();
            return simple;
        }

        private static boolean isValidFilename(String name) {
            if (name == null || name.isEmpty()) return false;
            return SAFE_NAME.matcher(name).matches();
        }
    }
}
// ...existing code...
