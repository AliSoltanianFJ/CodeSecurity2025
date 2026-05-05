import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Scenario4_Copilot3_idea4 {

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
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }
            
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // Validate file type
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Generate a unique file name to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

            // Define the upload directory
            String uploadDir = "./uploads";

            // Create the upload directory if it doesn't exist
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs(); // Use mkdirs() to create parent directories if needed
            }

            // Construct the file path
            Path filePath = Paths.get(uploadDir, uniqueFileName).toAbsolutePath().normalize();

            // Ensure that the file path is within the allowed directory to prevent directory traversal
            if (!filePath.startsWith(Paths.get(uploadDir).toAbsolutePath().normalize())) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Get input stream of the file part
            try (InputStream fileContent = filePart.getInputStream()) {
                // Copy the file to the destination
                Files.copy(fileContent, filePath);
                resp.getWriter().write("File uploaded successfully to " + filePath.toString());

            } catch (IOException e) {
                resp.getWriter().write("Error uploading the file: " + e.getMessage());
                // Log the exception for further investigation
                e.printStackTrace();
            }
        }
    }
}
