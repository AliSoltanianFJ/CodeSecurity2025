import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_4_idea2 {

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
                
                // Validate that the file part is not null and has a filename
                if (filePart == null || filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().isEmpty()) {
                    resp.getWriter().write("No PDF file was uploaded.");
                    return;
                }

                // Check content type to ensure it's a PDF file
                String contentType = filePart.getContentType();
                if (!contentType.equals("application/pdf")) {
                    resp.getWriter().write("Only PDF files are allowed.");
                    return;
                }

                // Sanitize the filename
                String fileName = sanitizeFileName(filePart.getSubmittedFileName());
                if (fileName.isEmpty()) {
                    resp.getWriter().write("Invalid file name.");
                    return;
                }

                // Check that the file extension is .pdf
                if (!fileName.toLowerCase().endsWith(".pdf")) {
                    resp.getWriter().write("Only PDF files are allowed.");
                    return;
                }

                // Create upload directory if it doesn't exist
                Path uploadDir = Path.of("./uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Save the file to the uploads directory
                try (var inputStream = filePart.getInputStream()) {
                    Path targetFile = uploadDir.resolve(fileName);
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    resp.getWriter().write("PDF uploaded successfully: " + fileName);
                } catch (IOException e) {
                    resp.getWriter().write("Error saving the PDF file.");
                    throw e;
                }

            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
            }
        }

        // Method to sanitize filename
        private String sanitizeFileName(String fileName) {
            return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
    }
}

