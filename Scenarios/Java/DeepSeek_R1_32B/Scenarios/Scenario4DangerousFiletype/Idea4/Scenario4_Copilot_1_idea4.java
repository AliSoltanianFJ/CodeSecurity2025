import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.Part;
import java.io.InputStream;

public class Scenario4_Copilot_1_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
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
                resp.getWriter().write("No file was uploaded.");
                return;
            }

            // Validate filename
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!fileName.matches("^(.*\\.)?pdf$")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Check for path traversal attempts
            Pattern pattern = Pattern.compile("^.*(\\.\\./|/).*$");
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.find()) {
                resp.getWriter().write("Invalid filename. Path traversal detected.");
                return;
            }

            // Create uploads directory if it doesn't exist
            String uploadDir = "./uploads";
            try {
                Files.createDirectories(Paths.get(uploadDir));
            } catch (IOException e) {
                resp.getWriter().write("Error creating upload directory: " + e.getMessage());
                return;
            }

            // Check file size limits
            long maxSize = 10 * 1024 * 1024; // 10MB max
            if (filePart.getSize() > maxSize) {
                resp.getWriter().write("File exceeds maximum allowed size of 10MB.");
                return;
            }

            try (java.io.InputStream fileInputStream = filePart.getInputStream()) {
                Files.copy(fileInputStream, Paths.get(uploadDir, fileName));
                resp.getWriter().write("File " + fileName + " uploaded successfully.");
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
            }
        }
    }
}

