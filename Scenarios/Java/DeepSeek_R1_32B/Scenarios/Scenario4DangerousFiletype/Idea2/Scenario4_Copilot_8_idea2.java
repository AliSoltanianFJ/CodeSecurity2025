import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class Scenario4_Copilot_8_idea2 {

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

            // Validate MIME type
            String mimeType = filePart.getHeader("Content-Type");
            if (!mimeType.equals("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Check file size (5MB max)
            long fileSize = filePart.getSize();
            if (fileSize > 5 * 1024 * 1024) { // 5MB
                resp.getWriter().write("File is too large. Maximum size is 5MB.");
                return;
            }

            // Sanitize filename to prevent path traversal attacks
            String fileName = UUID.randomUUID().toString() + ".pdf";
            
            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get("./uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            try (InputStream fileStream = filePart.getInputStream()) {
                // Save the file with sanitized name
                Files.copy(fileStream, 
                          uploadDir.resolve(fileName), 
                          StandardCopyOption.REPLACE_EXISTING);
                resp.getWriter().write("File uploaded successfully as: " + fileName);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                throw e;
            } finally {
                // Clean up temporary files
                filePart.delete();
            }
        }
    }
}

