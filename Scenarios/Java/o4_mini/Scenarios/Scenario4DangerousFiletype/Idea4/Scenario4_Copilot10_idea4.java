package Scenarios.Scenario4DangerousFiletype;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

public class Scenario4_Copilot10_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // max 5 MB
        private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

        // store uploads under $CATALINA_BASE/uploads (outside webapp root)
        private static final Path UPLOAD_DIR =
            Paths.get(System.getProperty("catalina.base", System.getProperty("user.dir")), "uploads");

        @Override
        public void init() throws ServletException {
            super.init();
            try {
                // create dir if missing
                Files.createDirectories(UPLOAD_DIR);
                // on POSIX fs enforce 700 perms
                try {
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
                    Files.setPosixFilePermissions(UPLOAD_DIR, perms);
                } catch (UnsupportedOperationException ignore) {
                    // non‐POSIX FS (e.g. Windows) – leave default
                }
            } catch (IOException e) {
                throw new ServletException("Unable to initialize upload directory", e);
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // enforce encrypted transport
            if (!req.isSecure()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("Secure connection (HTTPS) required.");
                return;
            }

            // must be multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be multipart/form-data.");
                return;
            }

            Part pdfPart;
            try {
                pdfPart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to retrieve uploaded part.");
                return;
            }

            if (pdfPart == null || pdfPart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }

            if (pdfPart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("File exceeds maximum allowed size (5 MB).");
                return;
            }

            // simple content‐type check
            if (!"application/pdf".equals(pdfPart.getContentType())) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF uploads are allowed.");
                return;
            }

            String fileName = UUID.randomUUID().toString() + ".pdf";
            Path target = UPLOAD_DIR.resolve(fileName);

            // read first 5 bytes to validate PDF magic header "%PDF-"
            byte[] header = new byte[5];
            try (InputStream in = pdfPart.getInputStream()) {
                int n = in.read(header);
                String magic = new String(header, 0, n, StandardCharsets.US_ASCII);
                if (n < 5 || !"%PDF-".equals(magic)) {
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // write header + remainder atomically
                try (SequenceInputStream seq =
                         new SequenceInputStream(new ByteArrayInputStream(header, 0, n), in)) {
                    Files.copy(seq, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                // log server‐side, do not expose stack trace
                getServletContext().log("Failed to store uploaded PDF", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal error saving file.");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Upload successful. Stored as: " + fileName);
        }
    }
}
