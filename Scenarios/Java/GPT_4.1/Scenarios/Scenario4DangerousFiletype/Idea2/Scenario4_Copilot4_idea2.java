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

public class Scenario4_Copilot4_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to save uploads
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension
        private static final String ALLOWED_EXTENSION = ".pdf";
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Filename sanitization pattern (alphanumeric, dash, underscore, dot)
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

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

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File is too large.");
                return;
            }

            // Get submitted filename and sanitize
            String submittedFileName = getFileName(filePart);
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize filename
            String safeFileName = Paths.get(submittedFileName).getFileName().toString();
            if (!FILENAME_PATTERN.matcher(safeFileName).matches()) {
                resp.getWriter().write("Invalid filename.");
                return;
            }

            // Prevent path traversal
            if (safeFileName.contains("..")) {
                resp.getWriter().write("Invalid filename.");
                return;
            }

            // Ensure upload directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Could not create upload directory.");
                return;
            }

            // Save file securely
            Path filePath = uploadDir.resolve(safeFileName).normalize();
            // Ensure file is within upload directory
            if (!filePath.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            try (InputStream fileContent = filePart.getInputStream()) {
                // Check PDF magic number (first bytes)
                byte[] header = new byte[4];
                int read = fileContent.read(header, 0, 4);
                if (read < 4 || !(header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46)) { // %PDF
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // Reset stream to start
                fileContent.close();
                try (InputStream fileContent2 = filePart.getInputStream()) {
                    Files.copy(fileContent2, filePath);
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        // Helper to get submitted filename
        private String getFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return filename;
                }
            }
            return null;
        }
    }
}
