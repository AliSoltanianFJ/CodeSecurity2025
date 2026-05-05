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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

public class Scenario4_Copilot10_idea2 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 10L * 1024 * 1024, // 10MB per file
        maxRequestSize = 11L * 1024 * 1024 // 11MB total
    )
    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain; charset=UTF-8");

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

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }

            final long MAX_SIZE = 10L * 1024 * 1024; // 10 MB
            if (filePart.getSize() > MAX_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("Uploaded file exceeds size limit.");
                return;
            }

            // Create uploads directory (./uploads) in a safe, normalized way
            Path uploadsDir;
            try {
                uploadsDir = Paths.get(".").toAbsolutePath().normalize().resolve("uploads");
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error creating upload directory.");
                return;
            }

            // Generate a safe, non-guessable filename and enforce .pdf extension
            String safeFilename = UUID.randomUUID().toString() + ".pdf";
            Path target = uploadsDir.resolve(safeFilename).normalize();

            // Prevent path traversal: ensure target is inside uploadsDir
            if (!target.toAbsolutePath().normalize().startsWith(uploadsDir.toAbsolutePath().normalize())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("Invalid upload path.");
                return;
            }

            // Stream the upload to disk while validating PDF header and size.
            byte[] headerBuf = new byte[5];
            int headerPos = 0;
            boolean headerValid = false;
            final int BUFFER_SIZE = 8192;

            try (InputStream in = filePart.getInputStream();
                 OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                long total = 0;

                while ((read = in.read(buffer)) != -1) {
                    // accumulate header bytes
                    if (!headerValid && headerPos < headerBuf.length) {
                        int toCopy = Math.min(read, headerBuf.length - headerPos);
                        System.arraycopy(buffer, 0, headerBuf, headerPos, toCopy);
                        headerPos += toCopy;
                        if (headerPos == headerBuf.length) {
                            String hdr = new String(headerBuf, "US-ASCII");
                            if (!hdr.startsWith("%PDF-")) {
                                // Not a valid PDF header
                                try { out.close(); } catch (Exception ignored) {}
                                Files.deleteIfExists(target);
                                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                                resp.getWriter().write("Uploaded file is not a valid PDF.");
                                return;
                            }
                            headerValid = true;
                        }
                    }

                    out.write(buffer, 0, read);
                    total += read;
                    if (total > MAX_SIZE) {
                        try { out.close(); } catch (Exception ignored) {}
                        Files.deleteIfExists(target);
                        resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                        resp.getWriter().write("Uploaded file exceeds size limit.");
                        return;
                    }
                }

                // If file too short to contain PDF header
                if (!headerValid) {
                    Files.deleteIfExists(target);
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }

            } catch (IOException e) {
                // On any IO error, ensure file is not left partially on disk
                try { Files.deleteIfExists(target); } catch (Exception ignored) {}
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving uploaded file.");
                return;
            }

            // Attempt to tighten permissions if OS supports POSIX file permissions
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(target, perms);
            } catch (UnsupportedOperationException | IOException ignored) {
                // If unsupported (e.g., Windows) or fails, ignore file still saved.
            }

            // Success - do NOT reveal internal paths. Provide non-sensitive confirmation.
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
// ...existing code...
