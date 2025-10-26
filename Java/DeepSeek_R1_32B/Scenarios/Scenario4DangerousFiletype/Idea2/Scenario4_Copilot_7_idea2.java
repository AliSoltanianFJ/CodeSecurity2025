import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;

public class Scenario4_Copilot_7_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            try {
                Part filePart = req.getPart("pdf");
                
                // Validate the file part
                if (filePart == null || filePart.getSize() == 0) {
                    resp.getWriter().write("No file uploaded or empty file.");
                    return;
                }

                // Check if the file is a PDF
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                String contentType = filePart.getContentType();

                if (!"application/pdf".equals(contentType) && !fileName.toLowerCase().endsWith(".pdf")) {
                    resp.getWriter().write("Only PDF files are allowed.");
                    return;
                }

                // Sanitize filename
                String safeFileName = sanitizeFileName(fileName);
                
                // Create a unique file name to prevent overwrites
                String finalFileName = UUID.randomUUID() + "_" + safeFileName;

                // Define the upload directory
                String uploadDir = getServletContext().getRealPath("./uploads");
                
                // Ensure uploads directory exists
                if (!Files.exists(Paths.get(uploadDir))) {
                    Files.createDirectories(Paths.get(uploadDir));
                }

                // Check file size limit (e.g., 10MB)
                long fileSizeLimit = 10 * 1024 * 1024; // 10MB
                if (filePart.getSize() > fileSizeLimit) {
                    resp.getWriter().write("File exceeds maximum allowed size of 10MB.");
                    return;
                }

                // Save the file
                Path uploadPath = Paths.get(uploadDir, finalFileName);
                
                try (BufferedInputStream inputStream = new BufferedInputStream(filePart.getInputStream());
                     FileOutputStream outputStream = new FileOutputStream(uploadPath.toFile())) {
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                resp.getWriter().write("File uploaded successfully: " + finalFileName);
                
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
            } catch (IOException e) {
                resp.getWriter().write("An error occurred while processing the file upload.");
                // Log exception for debugging
                System.err.println(e.getMessage());
            }
        }

        private String sanitizeFileName(String fileName) {
            return fileName.replaceAll("[^a-zA-Z0-9\\-_\\.]", "");
        }
    }
}

