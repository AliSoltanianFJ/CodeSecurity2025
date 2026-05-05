// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.io.InputStream;
import java.nio.file.StandardOpenOption;

public class Scenario4_Copilot8_idea4 {

    @MultipartConfig
    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid form encoding.");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file provided.");
                return;
            }

            final long MAX_SIZE = 10L * 1024 * 1024; // 10 MB
            if (filePart.getSize() > MAX_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File too large.");
                return;
            }

            String submitted = filePart.getSubmittedFileName();
            if (submitted == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing filename.");
                return;
            }
            String name = Paths.get(submitted).getFileName().toString(); // strip path
            if (name.length() > 100 || name.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid filename.");
                return;
            }
            String lower = name.toLowerCase();
            if (!lower.endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }
            if (!name.matches("^[A-Za-z0-9._-]+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Filename contains invalid characters.");
                return;
            }

            Path uploads = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploads);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error.");
                return;
            }

            Path tempFile = null;
            try (InputStream in = filePart.getInputStream()) {
                tempFile = Files.createTempFile("upload-", ".tmp");
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

                byte[] header = new byte[5];
                try (InputStream tin = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                    int read = tin.read(header);
                    if (read < 4 || !(header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F')) {
                        Files.deleteIfExists(tempFile);
                        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                        resp.getWriter().write("File content is not a valid PDF.");
                        return;
                    }
                }

                String safeName = UUID.randomUUID().toString() + "_" + name;
                Path target = uploads.resolve(safeName).normalize();
                if (!target.getParent().equals(uploads)) {
                    Files.deleteIfExists(tempFile);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Invalid target path.");
                    return;
                }

                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("File uploaded.");
                return;
            } catch (IOException e) {
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
                }
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error.");
                return;
            }
        }
    }
}
