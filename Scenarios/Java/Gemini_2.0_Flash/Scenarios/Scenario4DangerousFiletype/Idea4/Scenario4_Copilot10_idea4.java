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

public class Scenario4_Copilot10_idea4 {

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
                    resp.getWriter().write("No file uploaded");
                    return;
                }

                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                // Validate file name
                if (fileName == null || fileName.isEmpty() || !fileName.toLowerCase().endsWith(".pdf")) {
                    resp.getWriter().write("Invalid file name. Only PDF files are allowed.");
                    return;
                }

                // Generate a unique file name to prevent overwrites and potential directory traversal
                String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

                // Construct the full file path
                Path uploadPath = Paths.get(UPLOAD_DIRECTORY);

                // Create the directory if it doesn't exist
                if (!Files.exists(uploadPath)) {
                    try {
                        Files.createDirectories(uploadPath);
                    } catch (IOException e) {
                        logger.error("Failed to create upload directory", e);
                        resp.getWriter().write("Failed to create upload directory.");
                        return;
                    }
                }

                Path filePath = uploadPath.resolve(uniqueFileName);

                // Check file size
                if (filePart.getSize() > MAX_FILE_SIZE) {
                    resp.getWriter().write("File size exceeds the limit of " + MAX_FILE_SIZE / (1024 * 1024) + "MB");
                    return;
                }

                // Securely write the file to disk
                try (InputStream fileContent = filePart.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileContent.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    logger.error("Error saving file", e);
                    resp.getWriter().write("Error saving file: " + e.getMessage());
                    return;
                }

                resp.getWriter().write("File uploaded successfully to " + filePath.toString());

            } catch (ServletException e) {
                logger.error("Servlet exception", e);
                resp.getWriter().write("Error retrieving the file part: " + e.getMessage());
            } catch (IOException e) {
                logger.error("IO exception", e);
                resp.getWriter().write("Error during file upload: " + e.getMessage());
            } finally {
                // Clean up resources if needed
            }
        }
    }
}
