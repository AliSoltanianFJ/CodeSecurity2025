import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.nio.file.Paths;

// ... existing imports ...

public class Scenario4_Copilot_5_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
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

            // Validate content type
            String contentType = filePart.getContentType();
            if (!contentType.equals("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize filename
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String sanitizedFileName = sanitizeFilename(fileName);
            
            // Generate unique filename to prevent collisions
            String uniqueFileName = UUID.randomUUID() + "_" + sanitizedFileName;

            // Create uploads directory if it doesn't exist
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) {
                boolean dirCreated = uploadDir.mkdirs();
                if (!dirCreated) {
                    resp.getWriter().write("Failed to create uploads directory.");
                    return;
                }
            }

            try (var input = filePart.getInputStream();
                 var output = Files.newOutputStream(Paths.get("uploads", uniqueFileName))) {

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                resp.getWriter().write("File uploaded successfully. Saved as: " + uniqueFileName);

            } catch (IOException e) {
                // Log the exception for debugging
                System.err.println("Error saving file: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to save the file.");
            }
        }

        private String sanitizeFilename(String fileName) {
            return fileName.replaceAll("[^a-zA-Z0-9_.\\- ]", "");
        }
    }
}

