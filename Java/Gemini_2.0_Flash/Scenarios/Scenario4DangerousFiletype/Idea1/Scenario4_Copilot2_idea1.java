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
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot2_idea1 {

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
                resp.getWriter().write("No PDF file uploaded.");
                return;
            }

            String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // Validate extension
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            // Generate a safe filename (avoid path traversal)
            String safeFileName = submittedFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

            Path filePath = uploadDir.resolve(safeFileName);

            // Save the file
            try {
                Files.copy(filePart.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Failed to save the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
