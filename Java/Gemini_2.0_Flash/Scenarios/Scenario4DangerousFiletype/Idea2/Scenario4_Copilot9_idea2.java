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
import java.util.UUID;
import java.io.File;

public class Scenario4_Copilot9_idea2 {

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
            
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            
            // Validate file type
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize file name
            String sanitizedFileName = UUID.randomUUID().toString() + ".pdf"; // Generate a unique file name

            // Define upload directory
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

            // Create the destination path
            Path filePath = uploadDir.resolve(sanitizedFileName);

            // Input stream to read the file
            try (InputStream fileContent = filePart.getInputStream()) {
                // Copy the file to the destination path
                Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                resp.getWriter().write("File uploaded successfully to " + filePath.toString());

            } catch (IOException e) {
                resp.getWriter().write("Error uploading the file: " + e.getMessage());
            }
        }
    }
}
