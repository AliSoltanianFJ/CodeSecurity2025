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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario4_Copilot1_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Logger for secure error logging (no sensitive info leaked to user)
        private static final Logger LOGGER = Logger.getLogger(UploadPDFServlet.class.getName());
        // Only allow filenames with safe characters (no path traversal, no special chars)
        private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
        // Limit file size to 10MB (adjust as needed)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure HTTPS is used (defense in depth, actual enforcement should be at server config)
            if (!req.isSecure()) {
                resp.getWriter().write("Connection must be secure (HTTPS).");
                return;
            }

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                LOGGER.log(Level.WARNING, "Error retrieving file part", e);
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate content type (only allow PDF)
            String contentType = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds limit.");
                return;
            }

            // Sanitize filename
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !SAFE_FILENAME.matcher(submittedFileName).matches() || submittedFileName.toLowerCase().endsWith(".exe")) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Ensure file has .pdf extension
            if (!submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("File must have a .pdf extension.");
                return;
            }

            // Save file securely
            Path uploadDir = Paths.get("./uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create upload directory", e);
                resp.getWriter().write("Internal server error.");
                return;
            }

            // Use a unique filename to prevent overwriting and race conditions
            String safeFileName = System.currentTimeMillis() + "_" + submittedFileName;
            Path filePath = uploadDir.resolve(safeFileName);

            // Save file atomically
            try {
                // Limit read size to MAX_FILE_SIZE
                try (var inputStream = filePart.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save file", e);
                resp.getWriter().write("Failed to save file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Securely extract the submitted file name from the Part header.
         * Prevents header injection and path traversal.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path information (defense in depth)
                    filename = Paths.get(filename).getFileName().toString();
                    return filename;
                }
            }
            return null;
        }
    }
}
