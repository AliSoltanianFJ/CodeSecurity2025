import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.UUID;

@MultipartConfig(                             // ensure container enforces multipart handling
    fileSizeThreshold = 1024 * 1024,          // 1MB in memory threshold
    maxFileSize = 10L * 1024 * 1024,          // 10MB max per file
    maxRequestSize = 12L * 1024 * 1024        // 12MB total
)
public class Scenario4_Copilot4_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final String UPLOAD_DIR = "uploads";
        private static final SecureRandom RANDOM = new SecureRandom();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter writer = resp.getWriter();

            // 1. Ensure request is multipart/form-data
            if (!req.getContentType().startsWith("multipart/form-data")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Request must be multipart/form-data");
                return;
            }

            // 2. Retrieve the file part
            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (IllegalStateException e) {
                // request size > configured limits
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Uploaded file is too large");
                return;
            } catch (ServletException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Error parsing multipart request");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "No file uploaded");
                return;
            }

            // 3. Validate content type
            String contentType = filePart.getContentType();
            if (!"application/pdf".equals(contentType)) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF files are accepted");
                return;
            }

            // 4. Prepare upload directory
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                // try to lock down permissions on POSIX systems
                PosixFileAttributeView posixView =
                    Files.getFileAttributeView(uploadDir, PosixFileAttributeView.class);
                if (posixView != null) {
                    Files.setPosixFilePermissions(uploadDir,
                        PosixFilePermissions.fromString("rwx------"));
                }
            }

            // 5. Generate secure random filename
            String storedFileName = UUID.randomUUID().toString() + ".pdf";
            Path targetFile = uploadDir.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                // just in case normalize() did something unexpected
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Invalid file path");
                return;
            }

            // 6. Stream file, check PDF magic header, write to disk
            try (InputStream rawIn = filePart.getInputStream();
                 BufferedInputStream in = new BufferedInputStream(rawIn)) {

                in.mark(4);
                byte[] header = new byte[4];
                int read = in.read(header);
                in.reset();
                if (read < 4
                        || header[0] != '%'
                        || header[1] != 'P'
                        || header[2] != 'D'
                        || header[3] != 'F') {
                    resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        "Uploaded file is not a valid PDF");
                    return;
                }

                // copy the stream to the target file
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioe) {
                // do not leak internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to store file");
                return;
            }

            // 7. Success
            writer.println("File uploaded successfully as " + storedFileName);
        }
    }
}
