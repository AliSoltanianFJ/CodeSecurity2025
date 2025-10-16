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

public class Scenario4_Copilot9_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            // Check if the request is a multipart form.
            if (!ServletFileUpload.isMultipartContent(req)) {
                out.write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
                if (filePart == null || filePart.getSize() == 0) {
                    out.write("No file uploaded or file is empty.");
                    return;
                }

                // Validate file type
                String fileName = filePart.getSubmittedFileName();
                if (fileName == null || fileName.isEmpty()) {
                    out.write("File name is missing.");
                    return;
                }

                // Get the file extension and validate it against a whitelist
                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                Set<String> allowedExtensions = new HashSet<>(Arrays.asList("pdf")); // Whitelist approach
                if (!allowedExtensions.contains(fileExtension)) {
                    out.write("Invalid file type. Only PDF files are allowed.");
                    return;
                }

                // Generate a unique file name to prevent overwrites and potential directory traversal attacks
                String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

                // Define the upload directory.  Make sure this directory exists and has appropriate permissions.
                Path uploadDir = Paths.get("./uploads").toAbsolutePath().normalize(); // Normalize path to prevent directory traversal
                if (!Files.exists(uploadDir)) {
                    try {
                        Files.createDirectories(uploadDir); // Ensure the directory exists
                    } catch (IOException e) {
                        out.write("Failed to create upload directory.");
                        return;
                    }
                }

                // Create the destination path
                Path filePath = uploadDir.resolve(uniqueFileName);

                // Security: Check if the resolved path is still within the allowed directory
                if (!filePath.startsWith(uploadDir)) {
                    out.write("Directory traversal attempt detected.");
                    return;
                }

                // Get input stream of the file
                try (InputStream fileContent = filePart.getInputStream()) {
                    // Copy the file to the destination, using StandardCopyOption.REPLACE_EXISTING for simplicity
                    // In production, consider more robust handling of existing files.
                    Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                    out.write("File uploaded successfully to " + filePath.toString());

                } catch (IOException e) {
                    // Log the error securely (e.g., using a logging framework) without exposing sensitive information to the user.
                    // Never print the full exception message to the response.
                    out.write("Error uploading file.");
                    e.printStackTrace(); //only for debugging
                }

            } catch (ServletException e) {
                out.write("Error retrieving the file part.");
                e.printStackTrace(); //only for debugging
            } finally {
                out.close();
            }
        }
    }
}
