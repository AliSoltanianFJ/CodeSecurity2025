// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario4_Copilot4_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(UploadPDFServlet.class.getName());
        private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
        private static final SecureRandom RNG = new SecureRandom();
        private static final byte[] PDF_MAGIC = new byte[] { '%', 'P', 'D', 'F', '-' };

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                LOGGER.log(Level.WARNING, "ServletException while retrieving file part", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error retrieving the uploaded file.");
                return;
            }
            
            // -copilot next line-
            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }

            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("Uploaded file is too large. Maximum allowed is 10 MB.");
                return;
            }

            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                // Content type can be forged; we still check magic header below, but reject non-pdf content types early.
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Uploaded file must be a PDF.");
                return;
            }

            // Prepare uploads directory (relative to working directory)
            Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to create uploads directory", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error.");
                return;
            }

            // Generate a safe random filename with .pdf extension; do not use user-supplied names to avoid path traversal / encoding attacks.
            String safeFilename;
            {
                byte[] random = new byte[18]; // enough entropy
                RNG.nextBytes(random);
                String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
                safeFilename = encoded + ".pdf";
            }

            Path target = uploadDir.resolve(safeFilename);
            // Ensure target remains inside uploadDir
            if (!target.toAbsolutePath().normalize().startsWith(uploadDir.toAbsolutePath().normalize())) {
                LOGGER.severe("Resolved target path is outside upload directory");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error.");
                return;
            }

            // Read stream safely, validate PDF magic header and write to disk using buffered streams.
            try (InputStream inRaw = filePart.getInputStream();
                 BufferedInputStream in = new BufferedInputStream(inRaw);
                 OutputStream outRaw = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
                 BufferedOutputStream out = new BufferedOutputStream(outRaw)) {

                // Mark/reset support for BufferedInputStream: set readlimit >= PDF_MAGIC.length
                in.mark(PDF_MAGIC.length + 1);
                byte[] header = new byte[PDF_MAGIC.length];
                int read = in.read(header);
                if (read != PDF_MAGIC.length || !Arrays.equals(header, PDF_MAGIC)) {
                    // Not a valid PDF
                    try {
                        // attempt to delete partial file if created
                        Files.deleteIfExists(target);
                    } catch (IOException ex) {
                        LOGGER.log(Level.FINE, "Unable to delete invalid upload file", ex);
                    }
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // reset so we copy the entire stream
                in.reset();

                // Copy stream in safe chunks
                byte[] buffer = new byte[8192];
                int len;
                long written = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    written += len;
                    // Defensive: don't write more than the declared Part size
                    if (written > filePart.getSize() || written > MAX_FILE_SIZE) {
                        // something is wrong -> abort
                        try { Files.deleteIfExists(target); } catch (IOException ex) { LOGGER.log(Level.FINE, "delete failed", ex); }
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("Uploaded file size mismatch or exceeded limit.");
                        return;
                    }
                }
                out.flush();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "I/O error while saving uploaded file", e);
                // attempt to remove any partially written file
                try { Files.deleteIfExists(target); } catch (IOException ex) { LOGGER.log(Level.FINE, "delete failed", ex); }
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error while saving the file.");
                return;
            }

            // Success: do not disclose filesystem paths or user-supplied filenames to avoid information leakage/XSS.
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
// ...existing code...
