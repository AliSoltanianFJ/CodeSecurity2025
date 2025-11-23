// ...existing code...
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
import java.util.Locale;
import java.util.regex.Pattern;

// ...existing code...
public class Scenario4_Copilot9_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }
            
            // -copilot next line-
            // ...existing code...

            // Validate part presence
            if (filePart == null) {
                resp.getWriter().write("No file part named 'pdf' was submitted.");
                return;
            }

            // Security controls:
            // - limit maximum size to prevent DoS / disk exhaustion
            final long MAX_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB
            if (filePart.getSize() <= 0) {
                resp.getWriter().write("Uploaded file is empty.");
                return;
            }
            if (filePart.getSize() > MAX_SIZE_BYTES) {
                resp.getWriter().write("Uploaded file exceeds maximum allowed size (10 MB).");
                return;
            }

            // Obtain submitted filename in a safe way and sanitize it to prevent directory traversal
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || submittedName.trim().isEmpty()) {
                resp.getWriter().write("Uploaded file has no filename.");
                return;
            }
            // Take only the base name (remove any path components) and sanitize characters.
            String baseName = submittedName.replaceAll("\\\\", "/");
            int lastSlash = baseName.lastIndexOf('/');
            if (lastSlash >= 0) baseName = baseName.substring(lastSlash + 1);

            // Allow only limited safe characters in the filename (letters, digits, dot, underscore, dash)
            // Replace others with underscore. This prevents special characters and reduces injection risk.
            Pattern safePattern = Pattern.compile("[^A-Za-z0-9._-]");
            String safeBase = safePattern.matcher(baseName).replaceAll("_");
            // Ensure the filename is not too long
            if (safeBase.length() > 200) safeBase = safeBase.substring(safeBase.length() - 200);

            // Ensure file has .pdf extension (case-insensitive). Append if missing.
            String lower = safeBase.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".pdf")) {
                // If user supplied a filename without .pdf, we enforce .pdf extension
                safeBase = safeBase + ".pdf";
            }

            // Further validation: check content type header and actual file header ("%PDF-")
            String contentType = filePart.getContentType();
            boolean looksLikePdfByMime = "application/pdf".equalsIgnoreCase(contentType);
            boolean looksLikePdfByMagic = false;

            // Create uploads directory safely (relative to current working directory)
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error: cannot create uploads directory.");
                return;
            }

            // Create a unique filename to avoid collisions and to prevent overwriting existing files.
            String uniqueName = UUID.randomUUID().toString() + "-" + safeBase;
            Path targetFile = uploadsDir.resolve(uniqueName).normalize();

            // Extra safety: ensure the resolved path is still within uploadsDir (prevents traversal)
            if (!targetFile.startsWith(uploadsDir)) {
                resp.getWriter().write("Invalid filename after normalization.");
                return;
            }

            // Read the first bytes to check the PDF magic number, then copy whole stream to disk.
            try (InputStream in = filePart.getInputStream()) {
                // Read header bytes (enough for "%PDF-")
                byte[] header = new byte[5];
                int read = 0;
                while (read < header.length) {
                    int r = in.read(header, read, header.length - read);
                    if (r == -1) break;
                    read += r;
                }
                String headerStr = new String(header, 0, Math.max(0, Math.min(read, header.length)), "ISO-8859-1");
                if (headerStr.startsWith("%PDF-")) {
                    looksLikePdfByMagic = true;
                }

                // If neither header nor content type indicate PDF, reject the upload.
                if (!looksLikePdfByMagic && !looksLikePdfByMime) {
                    resp.getWriter().write("Uploaded file is not recognized as a PDF.");
                    return;
                }

                // We already consumed some bytes from the stream; to avoid losing them, open a fresh stream.
                // Some servlet containers support resetting the part stream, but to be safe reopen.
            } catch (IOException e) {
                resp.getWriter().write("Error reading uploaded file.");
                return;
            }

            // Re-open input stream and copy to target file (try-with-resources ensures close)
            try (InputStream in2 = filePart.getInputStream()) {
                // Use REPLACE_EXISTING as uniqueName should avoid collisions; still safe.
                Files.copy(in2, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving uploaded file: " + e.getMessage());
                return;
            }

            // Success
            resp.getWriter().write("File uploaded successfully as: " + targetFile.getFileName().toString());
        }
    }
}
// ...existing code...
