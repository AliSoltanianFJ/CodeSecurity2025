import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot8_idea2 {

    @MultipartConfig(
        fileSizeThreshold   = 1024 * 1024,        // 1MB in memory threshold
        maxFileSize         = 10 * 1024 * 1024,   // 10MB per file
        maxRequestSize      = 20 * 1024 * 1024    // 20MB total
    )
    public static class UploadPDFServlet extends HttpServlet {
        private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1. Must be multipart/form-data
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be multipart/form-data");
                return;
            }

            // 2. Retrieve file part
            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving file part");
                return;
            }
            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded");
                return;
            }

            // 3. Validate size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds 10MB limit");
                return;
            }

            // 4. Validate content type
            String contentType = filePart.getContentType();
            if (contentType == null || !"application/pdf".equals(contentType)) {
                resp.getWriter().write("Only PDF files are allowed");
                return;
            }

            // 5. Sanitize filename
            String submittedName = Paths.get(filePart.getSubmittedFileName())
                                        .getFileName()
                                        .toString();
            if (submittedName == null || !submittedName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid file name or extension");
                return;
            }
            // replace any suspicious characters
            String safeName = submittedName.replaceAll("[^A-Za-z0-9_.-]", "_");

            // 6. Determine upload directory and ensure it exists
            ServletContext ctx = getServletContext();
            Path uploadDir = Paths.get(ctx.getRealPath("/"), "uploads")
                                  .toAbsolutePath()
                                  .normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Cannot create upload directory");
                return;
            }

            // 7. Resolve final path and prevent traversal
            Path target = uploadDir.resolve(safeName).normalize();
            if (!target.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path");
                return;
            }

            // 8. Stream the file to disk safely
            try (InputStream in = filePart.getInputStream();
                 OutputStream out = Files.newOutputStream(
                     target, StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (FileAlreadyExistsException e) {
                resp.getWriter().write("A file with that name already exists");
                return;
            } catch (IOException e) {
                resp.getWriter().write("Error saving file");
                return;
            }

            // 9. Success
            resp.getWriter().write("File uploaded successfully");
        }
    }
}
