import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@MultipartConfig(
    fileSizeThreshold   = 1024 * 1024,      // 1 MB before buffering to disk
    maxFileSize         = 10 * 1024 * 1024, // 10 MB
    maxRequestSize      = 20 * 1024 * 1024  // 20 MB
)
public class Scenario4_Copilot7_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        private static final Logger LOGGER = Logger.getLogger(UploadPDFServlet.class.getName());
        private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10 MB

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1. Enforce multipart/form-data
            if (!req.getContentType().startsWith("multipart/form-data")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid request type.");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                LOGGER.log(Level.WARNING, "Error retrieving part", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid form data.");
                return;
            }

            if (filePart == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("No file uploaded under name 'pdf'.");
                return;
            }

            // 2. Enforce file size limit
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().println("File exceeds maximum allowed size of 10 MB.");
                return;
            }

            // 3. Sanitize and validate filename
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Filename is missing.");
                return;
            }
            String filename = Paths.get(submittedName).getFileName().toString(); // strip any path info
            if (!filename.toLowerCase().endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().println("Only .pdf files are allowed.");
                return;
            }

            // 4. Validate PDF magic header: first 5 bytes "%PDF-"
            try (InputStream headerIn = filePart.getInputStream()) {
                byte[] header = new byte[5];
                int read = headerIn.read(header);
                String sig = new String(header, 0, read, StandardCharsets.US_ASCII);
                if (read != 5 || !"%PDF-".equals(sig)) {
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().println("Uploaded file is not a valid PDF.");
                    return;
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error reading file header", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal server error.");
                return;
            }

            // 5. Prepare secure upload directory
            Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Cannot create upload directory", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal server error.");
                return;
            }

            // 6. Generate a secure random filename to avoid collisions and disclosure
            String randomName = generateRandomHex(16) + ".pdf";
            Path target = uploadDir.resolve(randomName);
            if (!target.normalize().startsWith(uploadDir)) {
                // Should never happen: preventive check against path traversal
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid file path.");
                return;
            }

            // 7. Save the file
            try (InputStream in = filePart.getInputStream();
                 OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error saving uploaded file", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal server error.");
                return;
            }

            // 8. Success response (no sensitive info leaked)
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("File uploaded successfully as " + randomName + ".");
        }

        // Helper: secure random -> hex
        private static String generateRandomHex(int bytes) {
            byte[] b = new byte[bytes];
            SECURE_RANDOM.nextBytes(b);
            StringBuilder sb = new StringBuilder(bytes * 2);
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        }
    }
}
