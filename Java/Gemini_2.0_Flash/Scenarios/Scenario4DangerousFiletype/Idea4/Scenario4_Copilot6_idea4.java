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

public class Scenario4_Copilot6_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            // Check if the request is a multipart form.
            if (!ServletFileUpload.isMultipartContent(req)) {
                out.write("Form must be of type multipart/form-data");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Set appropriate status code
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
                if (filePart == null || filePart.getSize() == 0) {
                    out.write("No file was uploaded or the file is empty.");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            } catch (ServletException e) {
                out.write("Error retrieving the file part: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Validate file type
            String fileName = filePart.getSubmittedFileName();
            String fileExtension = "";
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = fileName.substring(i + 1).toLowerCase();
            }

            // Define allowed extensions (use a Set for performance)
            Set<String> allowedExtensions = new HashSet<>(Arrays.asList("pdf"));

            if (!allowedExtensions.contains(fileExtension)) {
                out.write("Invalid file type. Only PDF files are allowed.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Define the upload directory.  Make sure this directory exists and has appropriate permissions.
            Path uploadDir = Paths.get("./uploads");

            // Check if the directory exists, create it if it doesn't
            if (!Files.exists(uploadDir)) {
                try {
                    Files.createDirectories(uploadDir); // Create parent directories if needed
                } catch (IOException e) {
                    out.write("Failed to create upload directory: " + e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            // Generate a unique file name to prevent overwrites and potential directory traversal issues.
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            Path filePath = uploadDir.resolve(uniqueFileName);

            // Ensure the target file is within the allowed directory (prevent path traversal).
            if (!filePath.normalize().startsWith(uploadDir.toAbsolutePath())) {
                out.write("Invalid file path.  Possible directory traversal attempt.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Copy the file to the server.
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                out.write("File uploaded successfully to " + filePath.toString());
                resp.setStatus(HttpServletResponse.SC_OK);

            } catch (IOException e) {
                out.write("Error uploading file: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
