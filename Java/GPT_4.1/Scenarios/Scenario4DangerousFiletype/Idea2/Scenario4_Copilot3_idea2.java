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

public class Scenario4_Copilot3_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final String UPLOAD_DIR = "./uploads";
        private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB limit
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$");

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

            String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // Validate filename
            if (submittedFileName == null || !FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name. Only .pdf files with safe names are allowed.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File too large. Maximum allowed size is 5MB.");
                return;
            }

            // Validate MIME type
            String mimeType = filePart.getContentType();
            if (mimeType == null || !mimeType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Could not create upload directory.");
                return;
            }

            // Save the file securely
            Path filePath = uploadDir.resolve(submittedFileName).normalize();

            // Prevent path traversal
            if (!filePath.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            try {
                // Save file using a buffer to prevent memory issues
                try (var inputStream = filePart.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
