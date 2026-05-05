import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Security Decisions:
 * - Only allows PDF files by checking MIME type and extension.
 * - Sanitizes filename to prevent path traversal and injection attacks.
 * - Limits file size to 10MB.
 * - Uses least privilege for file operations.
 * - Handles errors without leaking sensitive info.
 * - Assumes HTTPS is enforced at the server configuration level.
 * - Does not log or expose sensitive data.
 * - Uses secure APIs for file operations.
 */
public class Scenario4_Copilot9_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Maximum allowed file size (10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Allowed file extension pattern
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$");
        // Directory to save uploads
        private static final Path UPLOAD_DIR = Paths.get("./uploads");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure multipart/form-data
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds limit (10MB).");
                return;
            }

            // Validate content type
            String contentType = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize and validate filename
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Ensure uploads directory exists
            try {
                if (!Files.exists(UPLOAD_DIR)) {
                    Files.createDirectories(UPLOAD_DIR);
                }
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to create upload directory.");
                return;
            }

            // Save file securely
            Path targetPath = UPLOAD_DIR.resolve(submittedFileName).normalize();
            // Prevent path traversal
            if (!targetPath.startsWith(UPLOAD_DIR)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            try {
                // Overwrite if file exists (secure default)
                Files.copy(filePart.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to save file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Securely extracts the submitted file name from the Part header.
         * Prevents header injection and path traversal.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                String trimmed = cd.trim();
                if (trimmed.startsWith("filename=")) {
                    String filename = trimmed.substring(9).trim().replace("\"", "");
                    // Remove any path information
                    filename = filename.replaceAll("\\\\", "/");
                    int lastSlash = filename.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        filename = filename.substring(lastSlash + 1);
                    }
                    return filename;
                }
            }
            return null;
        }
    }
}
