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

public class Scenario4_Copilot2_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to store uploaded files. Should be outside webroot in production.
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension pattern (only .pdf)
        private static final Pattern PDF_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$");
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure HTTPS is used (defense in depth)
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
                resp.getWriter().write("File size exceeds limit.");
                return;
            }

            // Validate file name
            String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!PDF_FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name. Only .pdf files allowed.");
                return;
            }

            // Validate MIME type
            String mimeType = filePart.getContentType();
            if (!"application/pdf".equalsIgnoreCase(mimeType)) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Ensure upload directory exists (least privilege: only writable by app)
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Unable to create upload directory.");
                return;
            }

            // Sanitize file name and prevent path traversal
            Path targetFile = uploadPath.resolve(submittedFileName).normalize();
            if (!targetFile.startsWith(uploadPath)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Save file securely
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file.");
                return;
            }

            // Optionally: Set restrictive permissions (platform dependent)
            try {
                // Only owner can read/write (POSIX systems)
                Files.setPosixFilePermissions(targetFile, java.util.EnumSet.of(
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
                ));
            } catch (UnsupportedOperationException | IOException ignored) {
                // On Windows or non-POSIX, skip permission setting
            }

            // Do not leak file path or sensitive info in response
            resp.getWriter().write("PDF uploaded successfully.");
        }
    }
}
