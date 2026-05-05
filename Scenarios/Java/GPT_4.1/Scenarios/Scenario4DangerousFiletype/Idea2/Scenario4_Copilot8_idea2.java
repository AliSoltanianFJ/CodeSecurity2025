import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

public class Scenario4_Copilot8_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to save uploads
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension
        private static final String ALLOWED_EXTENSION = ".pdf";
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Filename sanitization pattern (alphanumeric, underscore, dash, dot)
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[\\w\\-. ]+$");

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
                resp.getWriter().write("File is too large. Max allowed size is 10MB.");
                return;
            }

            // Get submitted file name and sanitize
            String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Ensure file extension is .pdf
            if (!submittedFileName.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Prevent directory traversal
            if (submittedFileName.contains("..") || submittedFileName.contains(File.separator)) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Create uploads directory if it doesn't exist
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            // Save file securely
            File file = new File(uploadDir, submittedFileName);
            try (InputStream input = filePart.getInputStream();
                 OutputStream output = new FileOutputStream(file)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > MAX_FILE_SIZE) {
                        resp.getWriter().write("File is too large.");
                        file.delete();
                        return;
                    }
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            // Set file permissions to be readable only by owner
            try {
                Files.setPosixFilePermissions(file.toPath(), 
                    java.util.EnumSet.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, 
                                         java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException | IOException ignored) {
                // On Windows, Posix permissions are not supported; ignore
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
