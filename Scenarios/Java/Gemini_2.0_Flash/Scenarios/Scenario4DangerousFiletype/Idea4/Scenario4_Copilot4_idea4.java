import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scenario4_Copilot4_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final Logger logger = LoggerFactory.getLogger(UploadPDFServlet.class);
        private static final String UPLOAD_DIRECTORY = "./uploads";
        private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB limit

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
                if (filePart == null) {
                    resp.getWriter().write("No file uploaded.");
                    return;
                }

                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                // Validate file name to prevent path traversal
                if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                    resp.getWriter().write("Invalid file name.");
                    logger.warn("Attempted path traversal attack: " + fileName);
                    return;
                }

                // Validate file type (MIME type)
                String contentType = filePart.getContentType();
                if (contentType == null || !contentType.equals("application/pdf")) {
                    resp.getWriter().write("Only PDF files are allowed.");
                    logger.warn("Attempted upload of non-PDF file: " + contentType);
                    return;
                }

                // Validate file size
                if (filePart.getSize() > MAX_FILE_SIZE) {
                    resp.getWriter().write("File size exceeds the maximum limit of 5MB.");
                    logger.warn("Attempted upload of file exceeding size limit: " + filePart.getSize());
                    return;
                }

                // Create the uploads directory if it doesn't exist
                File uploadDir = new File(UPLOAD_DIRECTORY);
                if (!uploadDir.exists()) {
                    if (!uploadDir.mkdirs()) {
                        resp.getWriter().write("Failed to create upload directory.");
                        logger.error("Failed to create upload directory: " + UPLOAD_DIRECTORY);
                        return;
                    }
                }

                // Generate a unique file name to prevent overwrites
                String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                File uploadedFile = new File(uploadDir, uniqueFileName);

                // Securely write the file to disk
                try (InputStream fileContent = filePart.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(uploadedFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileContent.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    resp.getWriter().write("Error saving the file.");
                    logger.error("Error saving file: " + uniqueFileName, e);
                    return;
                }

                resp.getWriter().write("File uploaded successfully to " + UPLOAD_DIRECTORY + "/" + uniqueFileName);
                logger.info("File uploaded successfully: " + uniqueFileName);

            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                logger.error("Error retrieving file part", e);
            } finally {
                if (filePart != null) {
                    filePart.delete(); // Clean up any temporary files
                }
            }
        }
    }
}
