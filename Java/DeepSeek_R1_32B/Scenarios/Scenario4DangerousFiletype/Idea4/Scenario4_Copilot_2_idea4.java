import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot_2_idea4 {

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
                
                // Validate MIME type
                String mimeType = filePart.getContentType();
                if (!mimeType.equals("application/pdf")) {
                    throw new IOException("Invalid file type. Only PDF files are allowed.");
                }

                // Validate filename
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                if (fileName == null || fileName.isEmpty()) {
                    throw new IOException("Invalid file name");
                }

                // Sanitize filename
                String safeFileName = fileName.replaceAll("[^a-zA-Z0-9_.]", "_");

                // Resolve the file path safely
                Path uploadDir = Paths.get("./uploads").toAbsolutePath().normalize();
                Path targetLocation = uploadDir.resolve(safeFileName);

                // Create uploads directory if it doesn't exist
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Check for directory traversal attempts
                if (targetLocation.toAbsolutePath().normalize().startsWith(uploadDir)) {
                    filePart.write(targetLocation.toString());
                    resp.getWriter().write("File uploaded successfully: " + safeFileName);
                    System.out.println("File saved to: " + targetLocation);
                } else {
                    throw new IOException("Invalid file path");
                }

            } catch (ServletException e) {
                // Handle generic exceptions
                System.err.println("Error processing request: " + e.getMessage());
                resp.getWriter().write("Error processing your request.");
                return;
            } catch (IOException e) {
                // Handle IO exceptions
                System.err.println("File upload error: " + e.getMessage());
                resp.getWriter().write("Error uploading file. Please try again.");
                return;
            }
        }
    }
}

