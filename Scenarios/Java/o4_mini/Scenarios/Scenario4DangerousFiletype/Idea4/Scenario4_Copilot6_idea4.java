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
import java.util.logging.Logger;

public class Scenario4_Copilot6_idea4 {
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = Logger.getLogger(UploadPDFServlet.class.getName());
        private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
        private static final Path UPLOAD_DIR = Paths.get("uploads");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            if (!req.isSecure()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("Secure connection required.");
                return;
            }
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be multipart/form-data.");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                logger.warning("Error retrieving file part: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file upload.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File size exceeds limit.");
                return;
            }

            String originalName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!originalName.toLowerCase().endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF files allowed.");
                return;
            }

            try {
                Files.createDirectories(UPLOAD_DIR);
            } catch (IOException e) {
                logger.severe("Could not create upload directory: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error.");
                return;
            }

            String storedName = UUID.randomUUID().toString() + ".pdf";
            Path target = UPLOAD_DIR.resolve(storedName);

            // Verify PDF signature
            try (InputStream in = filePart.getInputStream()) {
                byte[] header = new byte[4];
                if (in.read(header) != 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Invalid PDF.");
                    return;
                }
            }

            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target);
                target.toFile().setReadable(true, true);
                target.toFile().setWritable(true, true);
            } catch (IOException e) {
                logger.severe("Error saving file: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Could not save file.");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Uploaded as " + storedName);
        }
    }
}
