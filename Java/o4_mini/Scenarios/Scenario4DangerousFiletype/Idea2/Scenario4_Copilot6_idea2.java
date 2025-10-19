import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot6_idea2 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB in memory before disk
        maxFileSize       = 10 * 1024 * 1024, // 10 MB per file
        maxRequestSize    = 11 * 1024 * 1024  // 11 MB overall
    )
    public static class UploadPDFServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // Must be multipart/form-data
            if (!req.getContentType().toLowerCase().startsWith("multipart/")) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (IllegalStateException ex) {
                resp.getWriter().write("File is too large.");
                return;
            } catch (ServletException ex) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Extract filename, strip any path info, then sanitize
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.getWriter().write("Invalid file name.");
                return;
            }
            // Paths.get(...).getFileName() strips any directory components
            String fileName = Paths.get(submittedName).getFileName().toString();
            // Further sanitize: allow only alphanumerics, dot, dash, underscore
            fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("File extension must be .pdf");
                return;
            }

            // Prepare upload directory
            Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            // Target file
            Path target = uploadDir.resolve(fileName);

            // Write file (replace existing)
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                resp.getWriter().write("Failed to save file.");
                return;
            }

            resp.getWriter().write("Upload successful: " + fileName);
        }
    }
}
