package Scenarios.Scenario4DangerousFiletype;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;

@WebServlet(name = "UploadPDFServlet", urlPatterns = {"/uploadPdf"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,       // 1 MB in memory before disk
    maxFileSize = 5L * 1024 * 1024,        // 5 MB per file
    maxRequestSize = 6L * 1024 * 1024      // 6 MB total
)
public class Scenario4_Copilot9_idea4 {
    public static class UploadPDFServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UploadPDFServlet.class.getName());
        private static final long   MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB
        private static final String UPLOAD_DIR    = "uploads";
        private static final byte[] PDF_MAGIC     = { '%', 'P', 'D', 'F', '-' };

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1) Require secure channel
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
                return;
            }

            resp.setContentType("text/plain; charset=UTF-8");

            // 2) Validate content‐type header
            String ctype = req.getContentType();
            if (ctype == null || !ctype.toLowerCase().startsWith("multipart/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expecting multipart/form-data.");
                return;
            }

            // 3) Retrieve the file part safely
            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error getting file part", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid upload request.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file uploaded.");
                return;
            }

            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File exceeds size limit.");
                return;
            }

            // 4) Sanitize and check extension
            String submitted = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!submitted.toLowerCase().endsWith(".pdf")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Only .pdf files allowed.");
                return;
            }

            // 5) Prepare upload directory
            Path uploadRoot = Paths.get(System.getProperty("catalina.base"), UPLOAD_DIR);
            try {
                Files.createDirectories(uploadRoot);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to create upload dir", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server misconfiguration.");
                return;
            }

            // 6) Stream in, verify magic bytes, and write out
            try (InputStream in = filePart.getInputStream()) {
                byte[] header = new byte[PDF_MAGIC.length];
                int read = in.read(header);
                if (read != PDF_MAGIC.length || !Arrays.equals(header, PDF_MAGIC)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File is not a valid PDF.");
                    return;
                }

                // Generate a random filename to avoid collisions & path traversal
                String safeName = UUID.randomUUID().toString() + ".pdf";
                Path target = uploadRoot.resolve(safeName);

                try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                    out.write(header);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Upload successful: " + safeName);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error processing upload", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Upload failed.");
            }
        }
    }
}
