import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class Scenario4_Copilot1_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            // 1. Must be multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            // 2. Get the file part
            Part filePart = req.getPart("pdf");
            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded");
                return;
            }

            // 3. Validate content type
            if (!"application/pdf".equals(filePart.getContentType())) {
                resp.getWriter().write("Uploaded file is not a PDF");
                return;
            }

            // 4. Check PDF magic header (“%PDF-”)
            try (InputStream is = filePart.getInputStream()) {
                byte[] header = new byte[5];
                int read = is.read(header);
                String sig = new String(header, StandardCharsets.US_ASCII);
                if (read < 5 || !"%PDF-".equals(sig)) {
                    resp.getWriter().write("Invalid PDF file");
                    return;
                }
            }

            // 5. Ensure upload directory exists
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 6. Sanitize original filename and prefix with UUID
            String submitted = filePart.getSubmittedFileName();
            String base = Paths.get(submitted).getFileName().toString()
                    .replaceAll("[^A-Za-z0-9._-]", "_");
            String filename = UUID.randomUUID() + "_" + base;

            // 7. Resolve path and ensure no traversal
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path");
                return;
            }

            // 8. Save the file
            try (InputStream is = filePart.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file");
                return;
            }

            resp.getWriter().write("File uploaded successfully as " + filename);
        }
    }
}
