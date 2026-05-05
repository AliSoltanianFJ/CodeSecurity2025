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
import java.io.InputStream;
import java.util.regex.Pattern;

public class Scenario4_Copilot7_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to save uploads
        private static final String UPLOAD_DIR = "./uploads";
        // Only allow filenames with safe characters
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

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

            // Validate content type
            String contentType = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate file name
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !FILENAME_PATTERN.matcher(submittedFileName).matches() || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Prevent path traversal
            String safeFileName = Paths.get(submittedFileName).getFileName().toString();

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }

            // Save file securely
            Path filePath = uploadDir.resolve(safeFileName);
            try (InputStream fileContent = filePart.getInputStream()) {
                // Limit file size to 10MB
                if (filePart.getSize() > 10 * 1024 * 1024) {
                    resp.getWriter().write("File too large. Max 10MB allowed.");
                    return;
                }
                Files.copy(fileContent, filePath);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        // Helper to get submitted file name safely
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path information
                    return filename.replaceAll("\\\\", "/").replaceAll(".*/", "");
                }
            }
            return null;
        }
    }
}
