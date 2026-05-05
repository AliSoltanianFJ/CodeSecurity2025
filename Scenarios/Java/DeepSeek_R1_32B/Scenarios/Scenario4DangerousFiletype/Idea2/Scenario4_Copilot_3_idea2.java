import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario4_Copilot_3_idea2 {

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

            String fileName = sanitizeFileName(filePart.getSubmittedFileName());
            if (fileName == null || !fileName.endsWith(".pdf")) {
                resp.getWriter().write("Invalid or no PDF file provided");
                return;
            }

            // Check file size
            long fileSize = filePart.getSize();
            if (fileSize > 10 * 1024 * 1024) {  // Max 10MB
                resp.getWriter().write("PDF file too large. Maximum allowed size is 10MB.");
                return;
            }

            try (InputStream fileStream = filePart.getInputStream()) {
                byte[] bytes = new byte[1024];
                int read;

                File uploadsDir = new File("uploads");
                if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory");
                    return;
                }

                Path targetLocation = Paths.get(uploadsDir.getAbsolutePath(), fileName);
                
                // Check for duplicate files
                if (targetLocation.toFile().exists()) {
                    resp.getWriter().write("A file with this name already exists.");
                    return;
                }

                try {
                    Files.copy(fileStream, targetLocation);
                } catch (Exception e) {
                    Logger.getLogger(UploadPDFServlet.class.getName()).log(Level.SEVERE, "Failed to save PDF", e);
                    resp.getWriter().write("Error saving the PDF file");
                    return;
                }

                resp.getWriter().write("PDF uploaded successfully");

            } catch (IOException e) {
                Logger.getLogger(UploadPDFServlet.class.getName()).log(Level.SEVERE, "File upload error", e);
                resp.getWriter().write("An error occurred while processing your request");
            }
        }

        private String sanitizeFileName(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return null;
            }

            // Remove any characters that are not alphanumeric or . _
            String sanitized = fileName.replaceAll("[^a-zA-Z0-9_.]", "");
            
            // Prevent directory traversal attacks
            if (sanitized.contains("../") || sanitized.contains(".." + File.separator)) {
                return null;
            }

            return sanitized;
        }
    }
}

