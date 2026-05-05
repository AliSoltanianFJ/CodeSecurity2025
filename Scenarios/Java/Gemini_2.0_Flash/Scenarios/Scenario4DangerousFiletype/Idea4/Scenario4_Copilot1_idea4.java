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

public class Scenario4_Copilot1_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            // Check if the request contains multipart content
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

                // Validate file type (check content type and file extension)
                String contentType = filePart.getContentType();
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                if (contentType == null || !contentType.equals("application/pdf") || !fileName.toLowerCase().endsWith(".pdf")) {
                    out.write("Only PDF files are allowed.");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                // Generate a unique file name to prevent overwrites and potential directory traversal
                String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                Path uploadDir = Paths.get("./uploads");

                // Create the upload directory if it doesn't exist
                if (!Files.exists(uploadDir)) {
                    try {
                        Files.createDirectories(uploadDir);
                    } catch (IOException e) {
                        out.write("Failed to create upload directory.");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }
                }

                Path filePath = uploadDir.resolve(uniqueFileName);

                // Use try-with-resources to ensure streams are closed after usage
                try (InputStream fileContent = filePart.getInputStream()) {
                    // Securely copy the file to the destination
                    Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
                    out.write("File uploaded successfully to " + filePath.toString());
                    resp.setStatus(HttpServletResponse.SC_OK);
                } catch (IOException e) {
                    out.write("Error saving the file: " + e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

            } catch (ServletException e) {
                out.write("Error retrieving the file part: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                out.close(); // Ensure the writer is closed
            }
        }
    }
}
