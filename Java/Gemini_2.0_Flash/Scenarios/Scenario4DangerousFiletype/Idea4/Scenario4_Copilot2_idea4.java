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

public class Scenario4_Copilot2_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            // Check if the request is a multipart request
            if (!ServletFileUpload.isMultipartContent(req)) {
                out.write("Form must be of type multipart/form-data");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Set appropriate status code
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
                if (filePart == null) {
                    out.write("No file uploaded with the name 'pdf'.");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            } catch (ServletException e) {
                out.write("Error retrieving the file part: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            // Validate file name
            if (fileName == null || fileName.isEmpty()) {
                out.write("File name is empty.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Check file extension against a whitelist
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            Set<String> allowedExtensions = new HashSet<>(Arrays.asList("pdf")); // Define allowed extensions
            if (!allowedExtensions.contains(fileExtension)) {
                out.write("File type not allowed. Only PDF files are permitted.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Define the upload directory
            Path uploadDir = Paths.get("./uploads");

            // Create the directory if it doesn't exist
            if (!Files.exists(uploadDir)) {
                try {
                    Files.createDirectories(uploadDir);
                } catch (IOException e) {
                    out.write("Failed to create upload directory: " + e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            // Generate a unique file name to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            Path filePath = uploadDir.resolve(uniqueFileName);

            // Ensure the destination path is within the upload directory to prevent directory traversal
            if (!filePath.normalize().startsWith(uploadDir.toAbsolutePath())) {
                out.write("Invalid file path.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Copy the file to the upload directory
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
