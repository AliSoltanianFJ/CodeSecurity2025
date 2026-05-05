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
import javax.servlet.http.Part;

public class Scenario4_Copilot_2_idea2 {

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
                
                // Validate that the part exists and is not empty
                if (filePart == null || filePart.getSize() == 0) {
                    resp.getWriter().write("No PDF file was uploaded.");
                    return;
                }

                // Extract the filename from the part
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                
                // Validate filename - only allow alphanumeric characters and dots
                if (!fileName.matches("[a-zA-Z0-9._]+")) {
                    resp.getWriter().write("Invalid filename. Only alphanumeric characters, underscores, and periods are allowed.");
                    return;
                }

                // Check that the file is a PDF
                String contentType = filePart.getContentType();
                if (contentType == null || !contentType.equals("application/pdf")) {
                    resp.getWriter().write("Only PDF files are allowed for upload.");
                    return;
                }

                // Validate the file extension
                if (!fileName.toLowerCase().endsWith(".pdf")) {
                    resp.getWriter().write("Invalid file extension. Only .PDF files are allowed.");
                    return;
                }

                // Create uploads directory if it doesn't exist
                Path uploadDir = Paths.get("./uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Sanitize the filename to prevent path traversal attacks
                String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9.]", "_");

                // Copy the file to the uploads directory
                Path destinationFile = uploadDir.resolve(sanitizedFileName);

                try (InputStream inputStream = filePart.getInputStream();
                     java.io.OutputStream outputStream = Files.newOutputStream(destinationFile)) {
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                resp.getWriter().write("PDF file uploaded successfully to ./uploads/" + sanitizedFileName);

            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part: " + e.getMessage());
                return;
            } catch (IOException e) {
                resp.getWriter().write("Error saving the PDF file: " + e.getMessage());
                throw e; // Re-throw IOException to ensure proper cleanup
            }
        }
    }
}

