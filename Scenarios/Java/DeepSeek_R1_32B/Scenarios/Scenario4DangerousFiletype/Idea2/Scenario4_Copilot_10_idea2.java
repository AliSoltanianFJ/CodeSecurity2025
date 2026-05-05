import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.*;

public class Scenario4_Copilot_10_idea2 {

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

            if (filePart == null) {
                resp.getWriter().write("No file selected or invalid form data.");
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            // Validate and sanitize filename to prevent path traversal attacks
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9_.]", "_");
            
            // Verify MIME type is application/pdf
            if (!"application/pdf".equals(filePart.getHeader("Content-Type"))) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            try {
                // Create uploads directory if it doesn't exist
                String uploadDir = "./uploads";
                Path uploadPath = Path.of(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Save the file
                Path targetFile = uploadPath.resolve(safeFileName);
                
                try (InputStream inputStream = filePart.getInputStream();
                     OutputStream outputStream = Files.newOutputStream(targetFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                resp.getWriter().write("Successfully uploaded: " + safeFileName);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
                throw new IOException("Failed to save uploaded PDF", e);
            } catch (Exception e) {
                resp.getWriter().write("Internal server error occurred.");
                throw e;
            }
        }
    }
}

