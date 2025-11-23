import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.io.InputStream;
import java.io.OutputStream;

public class Scenario4_Copilot7_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        private static final long MAX_SIZE = 10L * 1024 * 1024; // 10 MB
        private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid request");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid request");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded");
                return;
            }

            if (filePart.getSize() > MAX_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File too large");
                return;
            }

            String contentType = filePart.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPE.equalsIgnoreCase(contentType.trim())) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Unsupported file type");
                return;
            }

            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error");
                return;
            }

            Path tempFile = null;
            try (InputStream in = filePart.getInputStream()) {
                tempFile = Files.createTempFile("upload-", ".tmp");
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int r;
                    while ((r = in.read(buffer)) != -1) {
                        out.write(buffer, 0, r);
                    }
                }

                byte[] header = new byte[4];
                try (InputStream headerIn = Files.newInputStream(tempFile)) {
                    int read = headerIn.read(header);
                    if (read < 4) {
                        Files.deleteIfExists(tempFile);
                        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                        resp.getWriter().write("Unsupported file type");
                        return;
                    }
                }

                String sig = new String(header, "ISO-8859-1");
                if (!sig.startsWith("%PDF")) {
                    Files.deleteIfExists(tempFile);
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Unsupported file type");
                    return;
                }

                String filename = UUID.randomUUID().toString() + ".pdf";
                Path destination = uploadsDir.resolve(filename);
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);

                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("Uploaded");
                return;
            } catch (IOException e) {
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
                }
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error");
                return;
            }
        }
    }
}
