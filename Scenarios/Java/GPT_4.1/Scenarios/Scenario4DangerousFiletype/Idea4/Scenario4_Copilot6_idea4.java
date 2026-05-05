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

public class Scenario4_Copilot6_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory for uploads. Should be outside webroot in production.
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension pattern (only .pdf)
        private static final Pattern PDF_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$");
        // Maximum file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure HTTPS is used (defense in depth)
            if (!req.isSecure()) {
                resp.getWriter().write("Uploads must be sent over HTTPS.");
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
                resp.getWriter().write("File is too large. Maximum allowed size is 10MB.");
                return;
            }

            // Validate file name
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !PDF_FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name. Only .pdf files are allowed.");
                return;
            }

            // Validate content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Ensure uploads directory exists with least privilege
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    // Set directory permissions to owner only (platform dependent)
                    // On Windows, this is more complex; on Unix, use PosixFilePermissions
                }
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to create upload directory.");
                return;
            }

            // Sanitize file name to prevent path traversal
            Path targetFile = uploadDir.resolve(submittedFileName).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Save file securely
            try (InputStream fileContent = filePart.getInputStream()) {
                // Overwrite protection: fail if file exists
                if (Files.exists(targetFile)) {
                    resp.getWriter().write("File already exists.");
                    return;
                }
                Files.copy(fileContent, targetFile);
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to save file.");
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
                    // Remove any path information (IE/Edge may send full path)
                    filename = Paths.get(filename).getFileName().toString();
                    return filename;
                }
            }
            return null;
        }
    }
}
