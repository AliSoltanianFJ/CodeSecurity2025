import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class Scenario4_Copilot6_idea4 {

    @MultipartConfig
    public static class UploadPDFServlet extends HttpServlet {
        private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10 MB
        private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file provided");
                return;
            }

            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File too large");
                return;
            }

            String contentType = filePart.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPE.equalsIgnoreCase(contentType.trim())) {
                resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported file type");
                return;
            }

            Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            String filename = UUID.randomUUID().toString() + ".pdf";
            Path target = uploadDir.resolve(filename);

            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("ok");
        }
    }
}
