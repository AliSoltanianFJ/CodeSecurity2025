import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,    // 1MB
    maxFileSize = 10 * 1024 * 1024,     // 10MB
    maxRequestSize = 20 * 1024 * 1024   // 20MB
)
public class Scenario4_Copilot3_idea4 {
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
        // uploads directory is created once, with normalized, non‐traversable path
        private static final Path UPLOAD_DIR = Paths.get(".", "uploads")
                                                   .toAbsolutePath()
                                                   .normalize();

        static {
            try {
                Files.createDirectories(UPLOAD_DIR);
                // In a real deployment you would lock down permissions here (POSIX ACLs, etc.)
            } catch (IOException e) {
                // fail fast if uploads directory cannot be created
                throw new ExceptionInInitializerError("Cannot create upload directory");
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain");

            // enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
                return;
            }

            // must be multipart/form-data
            if (!req.getContentType().toLowerCase().startsWith("multipart/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Request must be multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (IllegalStateException | IOException | ServletException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Error processing uploaded file");
                return;
            }
            if (filePart == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Form field 'pdf' is missing");
                return;
            }

            long size = filePart.getSize();
            if (size <= 0 || size > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "File must be >0 bytes and ≤ " + MAX_FILE_SIZE);
                return;
            }

            // sanitize filename
            String submittedName = Paths.get(filePart.getSubmittedFileName())
                                        .getFileName()
                                        .toString();
            if (!submittedName.toLowerCase().endsWith(".pdf")) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                               "Only .pdf files are allowed");
                return;
            }

            // validate content type
            if (!"application/pdf".equals(filePart.getContentType())) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                               "Content-Type must be application/pdf");
                return;
            }

            // quick magic‐number check: %PDF
            try (InputStream header = filePart.getInputStream()) {
                byte[] magic = new byte[4];
                if (header.read(magic) != 4 ||
                    magic[0] != '%' || magic[1] != 'P' ||
                    magic[2] != 'D' || magic[3] != 'F') {
                    resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                                   "File is not a valid PDF");
                    return;
                }
            }

            // generate a secure, random filename
            String safeName = UUID.randomUUID().toString() + ".pdf";
            Path target = UPLOAD_DIR.resolve(safeName);

            // copy stream, replace if exists
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Failed to save file");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Upload successful: " + safeName);
        }
    }
}
