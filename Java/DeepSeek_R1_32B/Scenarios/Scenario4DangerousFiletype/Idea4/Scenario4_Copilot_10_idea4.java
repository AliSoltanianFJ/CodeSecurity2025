import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.InputStream;

public class Scenario4_Copilot_10_idea4 {

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

            // Validate content type
            if (!filePart.getContentType().equals("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // Sanitize file name to prevent path traversal attacks
            String sanitizedFileName = UUID.randomUUID().toString() + "_" +
                    fileName.replaceAll("[^a-zA-Z0-9]", "_");
            
            // Set maximum allowed file size (e.g., 5MB)
            long maxFileSize = 5 * 1024 * 1024;
            if (filePart.getSize() > maxFileSize) {
                resp.getWriter().write("File size exceeds the maximum allowed limit.");
                return;
            }

            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get("./uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            try (InputStream fileContent = filePart.getInputStream()) {
                Path targetFile = uploadDir.resolve(sanitizedFileName);
                Files.copy(fileContent, targetFile);
                resp.getWriter().write("PDF file uploaded successfully.");
            } catch (IOException e) {
                resp.getWriter().write("Error saving the PDF file.");
                throw e;
            }
        }
    }
}