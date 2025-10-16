import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Scenario4_Copilot7_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            // Check if the request is a multipart request
            if (!ServletFileUpload.isMultipartContent(req)) {
                out.println("Error: Form must be of type multipart/form-data");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Set appropriate status code
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
                if (filePart == null) {
                    out.println("Error: No file was uploaded with the name 'pdf'.");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            } catch (ServletException e) {
                out.println("Error retrieving the file part: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // Validate file name (avoid path traversal)
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                out.println("Error: Invalid file name. Avoid using relative paths or slashes.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Get input stream of the file
            InputStream fileContent = filePart.getInputStream();

            // Define upload directory
            Path uploadDir = Paths.get("./uploads").toAbsolutePath();

            // Ensure the directory exists, create if not
            if (!Files.exists(uploadDir)) {
                try {
                    Files.createDirectories(uploadDir);
                } catch (IOException e) {
                    out.println("Error: Could not create upload directory: " + e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            // Generate a unique file name to avoid collisions
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            Path filePath = uploadDir.resolve(uniqueFileName);

            // List of allowed file extensions
            Set<String> allowedExtensions = new HashSet<>(Arrays.asList("pdf"));

            // Get the file extension
            String fileExtension = "";
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = fileName.substring(i + 1).toLowerCase();
            }

            // Check file extension against allowed extensions
            if (!allowedExtensions.contains(fileExtension)) {
                out.println("Error: File type not allowed. Only PDF files are permitted.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Save the file to the server
            try {
                Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                out.println("File successfully uploaded to " + filePath.toString());
                resp.setStatus(HttpServletResponse.SC_OK);
            } catch (IOException e) {
                out.println("Error: Could not save file: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                // Clean up resources
                try {
                    fileContent.close();
                } catch (IOException e) {
                    // Log the error, but don't throw exception as the response has already been sent
                    System.err.println("Error closing file stream: " + e.getMessage());
                }
            }
        }
    }
}
