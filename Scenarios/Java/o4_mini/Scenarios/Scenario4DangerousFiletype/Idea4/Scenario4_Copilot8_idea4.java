import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
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
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

/**
 * Secure PDF upload servlet.
 * 
 * Security decisions:
 * - @MultipartConfig enforces file size limits (1 MB threshold, 10 MB max file).
 * - Validates multipart content, size, extension, magic bytes.
 * - Uses a UUID for stored filename to prevent collisions and directory traversal.
 * - Creates uploads dir with least privilege; applies POSIX perms if supported.
 * - Generic error messages to avoid leaking internal state.
 */
public class Scenario4_Copilot8_idea4 {
    @WebServlet(name = "UploadPDFServlet", urlPatterns = {"/uploadPdf"})
    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB in memory before temp file
        maxFileSize = 10 * 1024 * 1024,         // 10 MB max per file
        maxRequestSize = 11 * 1024 * 1024       // 11 MB max total request
    )
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        // ". /uploads" directory, normalized to prevent traversal
        private static final Path UPLOAD_DIR = Paths.get(".", "uploads")
                                                     .toAbsolutePath()
                                                     .normalize();

        @Override
        public void init() throws ServletException {
            try {
                Files.createDirectories(UPLOAD_DIR);
                // apply owner RW only if POSIX is supported
                try {
                    Files.setPosixFilePermissions(UPLOAD_DIR,
                        PosixFilePermissions.fromString("rwx------"));
                } catch (UnsupportedOperationException ignored) {
                    // Windows or non-POSIX FS: skip
                }
            } catch (IOException e) {
                throw new ServletException("Cannot initialize upload directory", e);
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Must be multipart/form-data
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Form must be multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
                if (filePart == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                   "Missing file part");
                    return;
                }
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Error processing upload");
                return;
            }

            // 2) Validate file size
            long size = filePart.getSize();
            if (size <= 0 || size > 10L * 1024 * 1024) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid file size");
                return;
            }

            // 3) Validate extension
            String submitted = filePart.getSubmittedFileName();
            if (submitted == null ||
                !submitted.toLowerCase().endsWith(".pdf")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "File name must end in .pdf");
                return;
            }

            // 4) Check PDF magic bytes
            try (InputStream in = filePart.getInputStream()) {
                byte[] header = new byte[4];
                if (in.read(header) != 4 ||
                    header[0] != '%' || header[1] != 'P' ||
                    header[2] != 'D' || header[3] != 'F') {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                   "Uploaded file is not a valid PDF");
                    return;
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Error reading uploaded file");
                return;
            }

            // 5) Generate safe filename and path
            String safeName = UUID.randomUUID().toString() + ".pdf";
            Path target = UPLOAD_DIR.resolve(safeName).normalize();
            if (!target.getParent().equals(UPLOAD_DIR)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                               "Invalid file path");
                return;
            }

            // 6) Persist the file with least privilege
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.setPosixFilePermissions(target,
                        PosixFilePermissions.fromString("rw-------"));
                } catch (UnsupportedOperationException ignored) {
                    // non-POSIX FS: skip
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Error saving file");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Upload successful: " + safeName);
        }
    }
}
