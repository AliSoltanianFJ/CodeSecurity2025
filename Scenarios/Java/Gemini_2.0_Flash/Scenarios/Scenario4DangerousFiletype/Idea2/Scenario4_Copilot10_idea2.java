import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario4_Copilot10_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
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
            
            String fileName = filePart.getSubmittedFileName();

            // Validate file name
            if (fileName == null || fileName.isEmpty()) {
                resp.getWriter().write("File name is invalid.");
                return;
            }

            // Sanitize file name to prevent path traversal
            fileName = sanitizeFileName(fileName);

            // Validate file extension
            if (!isValidFileExtension(fileName)) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Define the upload directory
            Path uploadDir = Paths.get("./uploads");

            // Create the upload directory if it doesn't exist
            if (!Files.exists(uploadDir)) {
                try {
                    Files.createDirectories(uploadDir);
                } catch (IOException e) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            // Create the file path
            Path filePath = uploadDir.resolve(fileName);

            // Check if the file already exists
            if (Files.exists(filePath)) {
                resp.getWriter().write("File already exists. Please rename your file.");
                return;
            }

            // Copy the file to the upload directory
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                resp.getWriter().write("File uploaded successfully!");
            } catch (IOException e) {
                resp.getWriter().write("Error uploading the file: " + e.getMessage());
            }
        }

        private String sanitizeFileName(String fileName) {
            // Remove any characters that are not alphanumeric or underscores or dots
            String cleanName = fileName.replaceAll("[^A-Za-z0-9\\._-]", "_");

            // Prevent directory traversal attacks
            cleanName = cleanName.replace("..", "");

            return cleanName;
        }

        private boolean isValidFileExtension(String fileName) {
            String extension = "";

            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i+1);
            }

            // Check if the file extension is pdf (case-insensitive)
            return "pdf".equalsIgnoreCase(extension);
        }
    }
}
