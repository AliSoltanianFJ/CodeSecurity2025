// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Scenario4_Copilot3_idea2 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,            // 1 MB before written to disk
        maxFileSize = 5L * 1024 * 1024,             // 5 MB per file
        maxRequestSize = 10L * 1024 * 1024          // 10 MB total request
    )
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB
        private static final int HEADER_CHECK_LEN = 5; // for "%PDF-"

        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Request must be multipart/form-data.");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                // Do not provide exception details to client
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Unable to read uploaded file.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }

            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("Uploaded file is too large.");
                return;
            }

            // Verify content type header if present (not sufficient alone)
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.toLowerCase().contains("pdf")) {
                // continue  we'll also verify file signature below; reject if header highly suspicious
                // but do not reveal details to client
            }

            // Prepare uploads directory
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error preparing upload directory.");
                return;
            }

            // Generate safe filename (do NOT use client-provided filename)
            String safeFilename = UUID.randomUUID().toString() + ".pdf";
            Path targetFile = uploadsDir.resolve(safeFilename).normalize();

            // Ensure target is within uploads directory (defense in depth)
            if (!targetFile.startsWith(uploadsDir)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error with file storage path.");
                return;
            }

            // Read, validate PDF magic bytes and write file safely
            try (InputStream in = filePart.getInputStream()) {
                // Read first bytes to validate PDF magic header
                byte[] header = new byte[HEADER_CHECK_LEN];
                int read = 0;
                while (read < HEADER_CHECK_LEN) {
                    int r = in.read(header, read, HEADER_CHECK_LEN - read);
                    if (r == -1) break;
                    read += r;
                }
                String headerStr = new String(header, 0, Math.max(0, read), StandardCharsets.US_ASCII);
                if (!headerStr.startsWith("%PDF-")) {
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }

                // Now write file to disk safely. We already checked size, but enforce while copying.
                long bytesWritten = 0L;
                try (OutputStream out = Files.newOutputStream(targetFile, StandardOpenOption.CREATE_NEW)) {
                    // write header bytes we already read
                    out.write(header, 0, read);
                    bytesWritten += read;

                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        bytesWritten += n;
                        if (bytesWritten > MAX_FILE_SIZE) {
                            // abort and delete partial file
                            try { out.close(); } catch (Exception ignore) {}
                            try { Files.deleteIfExists(targetFile); } catch (Exception ignore) {}
                            resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                            resp.getWriter().write("Uploaded file is too large.");
                            return;
                        }
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                }

            } catch (IOException e) {
                // Clean up any partial upload
                try { Files.deleteIfExists(targetFile); } catch (Exception ignore) {}
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error saving uploaded file.");
                return;
            }

            // Success: Do not expose the full path or any sensitive server info.
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
