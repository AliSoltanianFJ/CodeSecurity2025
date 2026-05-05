import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

public class Scenario4_Copilot1_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to save uploads
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension
        private static final String ALLOWED_EXTENSION = ".pdf";
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Filename sanitization pattern (only allow alphanumeric, dash, underscore, and dot)
        private static final Pattern FILENAME_SANITIZE = Pattern.compile("[^a-zA-Z0-9._-]");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            } catch (IOException e) {
                resp.getWriter().write("IO error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File is too large. Maximum allowed size is 10MB.");
                return;
            }

            // Get submitted filename and sanitize
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || submittedFileName.isEmpty()) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Sanitize filename
            String safeFileName = FILENAME_SANITIZE.matcher(submittedFileName).replaceAll("_");

            // Validate extension
            if (!safeFileName.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate MIME type
            String mimeType = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(mimeType)) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Ensure upload directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }

            // Prevent path traversal
            Path filePath = uploadPath.resolve(safeFileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Save the file securely
            try {
                // Use try-with-resources to prevent resource leaks
                try (var inputStream = filePart.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                resp.getWriter().write("Failed to save the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        // Helper method to get submitted file name in a safe way
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path information from the filename
                    return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1);
                }
            }
            return null;
        }
    }
}
