import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.FileUploadException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

public class Scenario4_Copilot_4_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final String UPLOAD_DIRECTORY = "./uploads";
        private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        private static final String[] ALLOWED_MIME_TYPES = { "application/pdf" };

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
                resp.getWriter().write("No file was uploaded.");
                return;
            }

            // Validate MIME type
            String mimeType = filePart.getContentType();
            if (!isMimeTypeAllowed(mimeType)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Check file size
            long fileSize = filePart.getSize();
            if (fileSize > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds the maximum allowed limit of 5MB.");
                return;
            }

            try {
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                fileName = sanitizeFilename(fileName);
                String uniqueId = UUID.randomUUID().toString();
                String targetFileName = uniqueId + "-" + fileName;

                // Ensure the uploads directory exists
                Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Save the file
                try (InputStream inputStream = filePart.getInputStream();
                     OutputStream outputStream = Files.newOutputStream(uploadPath.resolve(targetFileName))) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                resp.getWriter().write("File uploaded successfully: " + targetFileName);

            } catch (IOException | IllegalStateException e) {
                // Log the exception with more details in a production environment
                resp.getWriter().write("Error occurred while saving the file.");
            }
        }

        private boolean isMimeTypeAllowed(String mimeType) {
            for (String allowed : ALLOWED_MIME_TYPES) {
                if (mimeType.equals(allowed)) {
                    return true;
                }
            }
            return false;
        }

        private String sanitizeFilename(String fileName) {
            // Remove characters that could be used for directory traversal
            return fileName.replaceAll("[^a-zA-Z0-9\\-_\\.]", "");
        }
    }
}

