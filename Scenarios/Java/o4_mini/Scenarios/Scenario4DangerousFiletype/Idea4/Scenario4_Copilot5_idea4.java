import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class Scenario4_Copilot5_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        // Max upload size: 10 MB
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Directory for uploads (ideally outside webroot)
        private static final Path UPLOAD_DIR = Paths.get("./uploads").toAbsolutePath().normalize();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            // Verify multipart/form-data
            String contentTypeHeader = req.getContentType();
            if (contentTypeHeader == null || !contentTypeHeader.toLowerCase().startsWith("multipart/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Form must be multipart/form-data.");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                log("Failed to parse multipart request", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid upload request.");
                return;
            }

            if (filePart == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing form field 'pdf'.");
                return;
            }

            // Enforce size limit
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                               "File exceeds max size of " + (MAX_FILE_SIZE/(1024*1024)) + " MB.");
                return;
            }

            // Validate MIME type
            String mime = filePart.getContentType();
            if (!"application/pdf".equals(mime)) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Only PDF files allowed.");
                return;
            }

            // Sanitize filename and prevent path traversal
            String originalName = filePart.getSubmittedFileName();
            String safeName = Paths.get(originalName).getFileName().toString();  
            String uniqueName = UUID.randomUUID().toString() + "_" + safeName;

            try {
                // Ensure upload directory exists
                Files.createDirectories(UPLOAD_DIR);

                Path target = UPLOAD_DIR.resolve(uniqueName).normalize();
                if (!target.startsWith(UPLOAD_DIR)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                    return;
                }

                // Save file securely
                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }

                resp.getWriter().write("File uploaded successfully: " + uniqueName);
            } catch (IOException e) {
                log("Error saving uploaded file", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Upload failed.");
            }
        }
    }
}
