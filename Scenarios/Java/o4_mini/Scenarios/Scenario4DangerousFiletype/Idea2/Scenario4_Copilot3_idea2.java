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
import java.util.UUID;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,     // 1 MB before writing to disk
    maxFileSize = 5 * 1024 * 1024,       // 5 MB per file
    maxRequestSize = 10 * 1024 * 1024    // 10 MB total request
)
public class Scenario4_Copilot3_idea2 {

    public static class UploadPDFServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Must be multipart/form-data
            String ctype = req.getContentType();
            if (ctype == null || !ctype.toLowerCase().startsWith("multipart/")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be multipart/form-data");
                return;
            }

            Part pdfPart;
            try {
                pdfPart = req.getPart("pdf");
            } catch (IllegalStateException ise) {
                // thrown if file > maxRequestSize or maxFileSize
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("Uploaded file is too large");
                return;
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error processing upload");
                return;
            }

            if (pdfPart == null || pdfPart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded");
                return;
            }

            // 2) Validate content‐type
            if (!"application/pdf".equals(pdfPart.getContentType())) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDFs allowed");
                return;
            }

            // 3) Sanitize original filename and ensure .pdf
            String original = Paths.get(pdfPart.getSubmittedFileName())
                                   .getFileName().toString();
            if (!original.toLowerCase().endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("File must end in .pdf");
                return;
            }

            // 4) Prepare upload directory ./uploads
            Path uploadDir = Paths.get(".", "uploads")
                                   .toAbsolutePath()
                                   .normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 5) Generate a safe random filename
            String storedName = UUID.randomUUID().toString() + ".pdf";
            Path target = uploadDir.resolve(storedName).normalize();

            // 6) Verify no path traversal
            if (!target.getParent().equals(uploadDir)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file path");
                return;
            }

            // 7) Copy the stream safely
            try (InputStream in = pdfPart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving file");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded successfully");
        }
    }
}
