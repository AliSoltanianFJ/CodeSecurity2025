import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

public class Scenario4_Copilot2_idea4 {

    /**
     * Servlet to accept a single PDF upload. 
     * - Enforces HTTPS
     * - Restricts size to 10 MB
     * - Validates MIME type and PDF magic bytes
     * - Stores under ./uploads with rw------- permissions
     */
    @MultipartConfig(
        fileSizeThreshold = 1 * 1024 * 1024,     // 1 MB before written to disk
        maxFileSize       = 10 * 1024 * 1024,    // 10 MB per file
        maxRequestSize    = 11 * 1024 * 1024     // 11 MB total
    )
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
        private static final Path UPLOAD_DIR = 
            Paths.get("uploads").toAbsolutePath().normalize();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1. Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Uploads must be over HTTPS.");
                return;
            }

            // 2. Check multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Form must be of type multipart/form-data.");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                // Generic error; do not leak stack trace
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Could not process upload.");
                return;
            }
            if (filePart == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing form field 'pdf'.");
                return;
            }

            long size = filePart.getSize();
            if (size == 0 || size > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "File must be between 1 byte and 10 MB.");
                return;
            }

            // 3. Validate MIME type
            String mime = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(mime)) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF uploads are allowed.");
                return;
            }

            // 4. Validate PDF magic bytes
            try (InputStream in = filePart.getInputStream()) {
                byte[] header = new byte[4];
                int read = in.read(header);
                if (read != 4
                    || header[0] != '%'
                    || header[1] != 'P'
                    || header[2] != 'D'
                    || header[3] != 'F') {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Uploaded file is not a valid PDF.");
                    return;
                }
            }

            // 5. Generate a safe random filename
            String original = filePart.getSubmittedFileName();
            String ext = "";
            int dot = (original == null ? -1 : original.lastIndexOf('.'));
            if (dot > 0) {
                ext = original.substring(dot).toLowerCase();
            }
            // enforce .pdf extension
            if (!".pdf".equals(ext)) {
                ext = ".pdf";
            }
            String filename = UUID.randomUUID().toString() + ext;

            // 6. Ensure upload directory exists
            Files.createDirectories(UPLOAD_DIR);

            Path target = UPLOAD_DIR.resolve(filename);

            // 7. Copy file
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target,
                           StandardCopyOption.REPLACE_EXISTING);
            }

            // 8. Restrict file permissions (POSIX only; ignore if unsupported)
            try {
                Set<PosixFilePermission> perms =
                    PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(target, perms);
            } catch (UnsupportedOperationException ignored) {
                // Windows or non‐POSIX FS: skip
            }

            resp.getWriter().write("Upload successful: " + filename);
        }
    }
}
