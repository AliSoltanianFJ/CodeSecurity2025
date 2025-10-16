import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.util.UUID;
import java.io.PrintWriter;

public class Scenario4_Copilot3_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            if (!ServletFileUpload.isMultipartContent(req)) {
                out.println("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                out.println("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                out.println("No file uploaded.");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // Validate file type
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                out.println("Only PDF files are allowed.");
                return;
            }

            // Generate a unique file name to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

            // Define the upload directory
            Path uploadDir = Paths.get("./uploads");

            // Create the upload directory if it doesn't exist
            if (!Files.exists(uploadDir)) {
                try {
                    Files.createDirectories(uploadDir);
                } catch (IOException e) {
                    out.println("Failed to create upload directory.");
                    return;
                }
            }

            // Construct the file path
            Path filePath = uploadDir.resolve(uniqueFileName);

            // Input stream to read the file data
            try (InputStream fileContent = filePart.getInputStream()) {
                // Copy the file to the upload directory
                Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                out.println("File uploaded successfully to " + filePath.toString());

            } catch (IOException e) {
                out.println("Error uploading the file: " + e.getMessage());
            }
        }
    }
}
