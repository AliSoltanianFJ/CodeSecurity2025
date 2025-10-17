import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;

public class Scenario4_Copilot4_idea1 {

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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String submittedFileName = filePart.getSubmittedFileName();
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize filename to prevent directory traversal
            String safeFileName = Paths.get(submittedFileName).getFileName().toString();

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(".", "uploads");
            try {
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
            } catch (IOException e) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            // Save the file
            Path filePath = uploadDir.resolve(safeFileName);
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, filePath);
            } catch (IOException e) {
                resp.getWriter().write("Failed to save the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
