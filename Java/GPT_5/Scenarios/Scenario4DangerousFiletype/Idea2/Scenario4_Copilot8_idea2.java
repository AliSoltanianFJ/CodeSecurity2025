import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Scenario4_Copilot8_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be of type multipart/form-data");
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
            
            // ...existing code...
            // Validate part presence and size limits
            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }

            final long MAX_SIZE = 10L * 1024L * 1024L; // 10 MB limit
            if (filePart.getSize() > MAX_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("Uploaded file is too large.");
                return;
            }

            // Check content type hint (best-effort) and then verify PDF magic bytes
            String contentType = filePart.getContentType();
            if (contentType == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing content type.");
                return;
            }

            // Read first bytes to verify PDF signature ("%PDF-")
            byte[] header = new byte[5];
            try (InputStream in = filePart.getInputStream()) {
                int read = 0;
                while (read < header.length) {
                    int r = in.read(header, read, header.length - read);
                    if (r == -1) break;
                    read += r;
                }
                // reset stream by opening a new stream for the actual copy below
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error reading uploaded file.");
                return;
            }

            String headerStr = new String(header, StandardCharsets.US_ASCII);
            if (!headerStr.startsWith("%PDF-")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Uploaded file is not a valid PDF.");
                return;
            }

            // Prepare uploads directory (relative to current working directory ./uploads)
            Path uploadsDir;
            try {
                uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
                // Prevent directory traversal by normalizing and ensuring it ends with "uploads"
                if (!uploadsDir.getFileName().toString().equals("uploads")) {
                    // extremely defensive: fallback to a safe directory
                    uploadsDir = Paths.get(System.getProperty("user.home"), "uploads").toAbsolutePath().normalize();
                }
                Files.createDirectories(uploadsDir);
            } catch (IOException | SecurityException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Unable to prepare upload directory.");
                return;
            }

            // Generate a safe, non-guessable filename (do not trust user-supplied name)
            String storedFilename = UUID.randomUUID().toString() + ".pdf";
            Path targetFile = uploadsDir.resolve(storedFilename).normalize();

            // Extra safety: ensure targetFile is inside uploadsDir
            if (!targetFile.startsWith(uploadsDir)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Invalid storage path.");
                return;
            }

            // Copy the uploaded stream to disk using a fresh InputStream (avoid reusing previously read stream)
            try (InputStream in = filePart.getInputStream()) {
                // Use atomic copy; UUID ensures uniqueness so REPLACE is unnecessary, but keep safe flag
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | SecurityException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to store uploaded file.");
                return;
            }

            // Do not expose internal paths or user-provided filenames in the response to prevent information leakage or XSS
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("File uploaded successfully.");
            // ...existing code...
        }
    }
}
