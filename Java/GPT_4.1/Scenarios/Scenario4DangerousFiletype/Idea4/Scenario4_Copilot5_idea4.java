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
import java.util.UUID;

public class Scenario4_Copilot5_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Secure regex for validating PDF filenames (no path traversal, only safe chars)
        private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$");
        // Directory for uploads (should be outside webroot in production)
        private static final String UPLOAD_DIR = "./uploads";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure request is multipart
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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate content type (defense in depth, not just extension)
            String contentType = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate filename
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !SAFE_FILENAME.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Generate a safe, unique filename to avoid collisions and path traversal
            String safeFileName = UUID.randomUUID().toString() + ".pdf";

            // Ensure upload directory exists (least privilege: only allow writing here)
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                // Do not leak internal paths
                resp.getWriter().write("Server error: unable to create upload directory.");
                return;
            }

            // Save the file securely
            Path filePath = uploadPath.resolve(safeFileName);
            try {
                // Limit file size (e.g., max 10MB)
                final long MAX_FILE_SIZE = 10 * 1024 * 1024;
                if (filePart.getSize() > MAX_FILE_SIZE) {
                    resp.getWriter().write("File too large. Maximum allowed size is 10MB.");
                    return;
                }

                // Save file atomically
                try (var inputStream = filePart.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                resp.getWriter().write("Server error: unable to save file.");
                return;
            }

            // Success: do not reveal internal paths
            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Securely extract the submitted filename from the Part header.
         * Prevents path traversal and other attacks.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Only return the base name (strip any path info)
                    return Paths.get(filename).getFileName().toString();
                }
            }
            return null;
        }
    }
}
