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

public class Scenario4_Copilot6_idea2 {

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

            // Validate file type (ensure it's a PDF)
            if (!isValidPDF(fileName)) {
                resp.getWriter().write("Only PDF files are allowed.");
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

            // Construct the file path
            Path filePath = uploadDir.resolve(fileName);

            // Check if the file already exists
            if (Files.exists(filePath)) {
                resp.getWriter().write("File already exists.");
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
            // Remove any characters that are not alphanumeric or dots
            String cleanName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");

            // Prevent directory traversal attacks
            cleanName = cleanName.replace("..", "");

            return cleanName;
        }

        private boolean isValidPDF(String fileName) {
            String regex = "^[a-zA-Z0-9._-]+\\.pdf$";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(fileName);
            return matcher.matches();
        }
    }
}
