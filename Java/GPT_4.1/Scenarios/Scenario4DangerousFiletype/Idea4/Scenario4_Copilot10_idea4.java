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
 * Secure PDF upload servlet.
 * - Only allows PDF files.
 * - Sanitizes file names.
 * - Uses secure file operations.
 * - Handles errors without leaking sensitive info.
 * - Assumes HTTPS is enforced at the server level.
 */
public class Scenario4_Copilot10_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory for uploads. Should be outside webroot in production.
        private static final String UPLOAD_DIR = "./uploads";
        // Only allow .pdf extension (case-insensitive)
        private static final Pattern PDF_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$", Pattern.CASE_INSENSITIVE);

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
            } catch (IOException e) {
                resp.getWriter().write("IO error retrieving the file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate content type (defense-in-depth, not solely relied upon)
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize and validate file name
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !PDF_FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name. Only .pdf files with safe characters are allowed.");
                return;
            }

            // Prevent directory traversal
            String safeFileName = Paths.get(submittedFileName).getFileName().toString();

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error: unable to create upload directory.");
                return;
            }

            // Save file securely
            Path targetFile = uploadDir.resolve(safeFileName);
            // Prevent overwriting existing files (optional: add unique suffix)
            if (Files.exists(targetFile)) {
                resp.getWriter().write("File already exists.");
                return;
            }

            try {
                // Limit file size to 10MB (adjust as needed)
                if (filePart.getSize() > 10 * 1024 * 1024) {
                    resp.getWriter().write("File too large. Max 10MB allowed.");
                    return;
                }
                // Save file atomically
                Files.copy(filePart.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Server error: unable to save file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Extracts the submitted file name from the Part header.
         * @param part The file part.
         * @return The file name, or null if not found.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                cd = cd.trim();
                if (cd.startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path info (IE bug)
                    return filename.replaceAll(".*[\\\\/]", "");
                }
            }
            return null;
        }
    }
}
