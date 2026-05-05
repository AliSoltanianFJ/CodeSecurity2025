// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

public class Scenario4_Copilot1_idea2 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,   // 1MB
        maxFileSize = 10L * 1024 * 1024,   // 10MB per file
        maxRequestSize = 12L * 1024 * 1024 // 12MB total
    )
    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain; charset=UTF-8");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                // Do not expose internal details
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            final long MAX_SIZE = 10L * 1024 * 1024; // 10 MB hard limit

            if (filePart.getSize() > MAX_SIZE) {
                resp.getWriter().write("Uploaded file is too large.");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Basic filename validation: prevent path traversal by extracting only filename
            String safeSubmitted = Paths.get(submittedName).getFileName().toString();
            String lower = safeSubmitted.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".pdf")) {
                resp.getWriter().write("Only .pdf files are allowed.");
                return;
            }

            // Validate declared content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                // Don't fail only on content-type, but prefer strict check together with magic bytes
                // continue to magic check below
            }

            // Check PDF magic bytes ("%PDF-")
            try (InputStream is = filePart.getInputStream()) {
                byte[] header = new byte[5];
                int read = 0;
                while (read < header.length) {
                    int n = is.read(header, read, header.length - read);
                    if (n == -1) break;
                    read += n;
                }
                String headerStr = new String(header, 0, Math.max(0, read), "US-ASCII");
                if (!headerStr.startsWith("%PDF-")) {
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
            } catch (IOException e) {
                resp.getWriter().write("Error validating uploaded file.");
                return;
            }

            // Prepare uploads directory
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error creating upload directory.");
                return;
            }

            // Generate a secure filename (do not use user-supplied name directly)
            String storedFileName = UUID.randomUUID().toString() + ".pdf";
            Path target = uploadDir.resolve(storedFileName);

            // Stream copy with size enforcement
            byte[] buffer = new byte[8192];
            long total = 0;
            try (InputStream in = filePart.getInputStream();
                 OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {

                int len;
                while ((len = in.read(buffer)) != -1) {
                    total += len;
                    if (total > MAX_SIZE) {
                        // remove partial file
                        try { Files.deleteIfExists(target); } catch (IOException ignored) {}
                        resp.getWriter().write("Uploaded file exceeds maximum allowed size.");
                        return;
                    }
                    out.write(buffer, 0, len);
                }
                out.flush();
            } catch (IOException e) {
                // cleanup on error
                try { Files.deleteIfExists(target); } catch (IOException ignored) {}
                resp.getWriter().write("Error saving uploaded file.");
                return;
            }

            // Success: do not reveal internal file path or UUID to user
            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
// ...existing code...
