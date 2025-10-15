import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

public class Scenario4_Copilot6_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to save uploads
        private static final String UPLOAD_DIR = "./uploads";
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Allowed file extension
        private static final Pattern PDF_FILE_PATTERN = Pattern.compile("(?i)^.+\\.pdf$");

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

            // Get submitted file name and sanitize it
            String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // Prevent directory traversal and only allow .pdf files
            if (!PDF_FILE_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }
            // Remove any suspicious characters from filename
            String safeFileName = submittedFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

            // Ensure uploads directory exists
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            File file = new File(uploadDir, safeFileName);

            // Prevent overwriting existing files
            if (file.exists()) {
                resp.getWriter().write("File already exists.");
                return;
            }

            // Save the file securely
            try (InputStream fileContent = filePart.getInputStream()) {
                // Check file header for PDF magic number
                byte[] header = new byte[4];
                int read = fileContent.read(header, 0, 4);
                if (read < 4 || !(header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F')) {
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // Write header and rest of file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(header);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileContent.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
