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
import java.io.InputStream;

public class Scenario4_Copilot4_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory for uploads. Should be outside webroot in production.
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed filename pattern: only letters, numbers, underscores, hyphens, and dots.
        private static final Pattern FILENAME_SAFE = Pattern.compile("^[\\w\\-.]+$");
        // Max file size (e.g., 5MB)
        private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure HTTPS is used (defense-in-depth, actual enforcement should be at server config)
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
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File is too large. Maximum allowed size is 5MB.");
                return;
            }

            // Validate content type (defense-in-depth, not foolproof)
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate file extension and sanitize filename
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid file extension. Only .pdf files are allowed.");
                return;
            }
            // Remove path info and validate filename
            String safeFileName = Paths.get(submittedFileName).getFileName().toString();
            if (!FILENAME_SAFE.matcher(safeFileName).matches()) {
                resp.getWriter().write("Invalid filename.");
                return;
            }

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to create upload directory.");
                return;
            }

            // Save file securely, avoiding overwrite and directory traversal
            Path targetFile = uploadDir.resolve(safeFileName).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }
            if (Files.exists(targetFile)) {
                resp.getWriter().write("File already exists.");
                return;
            }

            // Check PDF magic number (first bytes) for basic validation
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] header = new byte[4];
                int read = fileContent.read(header, 0, 4);
                if (read < 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // Reset stream for full copy
                fileContent.close();
            } catch (IOException e) {
                resp.getWriter().write("Error reading uploaded file.");
                return;
            }

            // Save file
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Extracts the submitted filename from the Part header.
         * This method avoids directory traversal and strips path info.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path info
                    return Paths.get(filename).getFileName().toString();
                }
            }
            return null;
        }
    }
}
