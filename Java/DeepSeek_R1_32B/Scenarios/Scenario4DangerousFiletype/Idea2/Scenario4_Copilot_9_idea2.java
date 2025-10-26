import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_9_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
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
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file was uploaded.");
                return;
            }

            // Validate file type
            String contentType = filePart.getContentType();
            if (!contentType.startsWith("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Get the filename and sanitize it
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // Validate the file name to prevent directory traversal attacks
            Path uploadDirPath = Paths.get("./uploads");
            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
            }

            // Create a new file with sanitized name in the uploads directory
            Path filePath = uploadDirPath.resolve(fileName);

            try (java.io.InputStream is = filePart.getInputStream()) {
                Files.copy(is, filePath);
                resp.getWriter().write("File uploaded successfully.");
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}

