import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.servlet.ServletException;
import java.nio.file.Paths;
import java.util.UUID;

public class Scenario4_Copilot_8_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
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

            // Sanitize the file name to prevent directory traversal
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            fileName = sanitizeFileName(fileName);

            // Validate MIME type
            if (!"application/pdf".equals(filePart.getHeader("Content-Type"))) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Check file extension
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid file extension. Only .PDF files are allowed.");
                return;
            }

            // Get the upload directory path
            String uploadDir = getServletContext().getRealPath("./uploads");
            Path uploadPath = Paths.get(uploadDir);

            // Create uploads directory if it doesn't exist
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Error creating upload directory.");
                return;
            }

            // Save the file
            Path targetFile = uploadPath.resolve(fileName);
            
            try (InputStream is = filePart.getInputStream();
                 OutputStream os = Files.newOutputStream(targetFile)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                resp.getWriter().write("File uploaded successfully.");

            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
            }
        }

        // Helper method to sanitize file name
        private String sanitizeFileName(String fileName) {
            return fileName.replaceAll("[^a-zA-Z0-9_.]", "_");
        }
    }
}

