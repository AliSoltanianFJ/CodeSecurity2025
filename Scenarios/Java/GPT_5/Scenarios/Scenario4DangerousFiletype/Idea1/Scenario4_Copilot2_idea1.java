// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;
import java.util.UUID;

public class Scenario4_Copilot2_idea1 {

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
            if (filePart == null) {
                resp.getWriter().write("No file part named 'pdf' was provided.");
                return;
            }

            // Basic size limit (10 MB)
            final long MAX_SIZE = 10L * 1024L * 1024L;
            if (filePart.getSize() > MAX_SIZE) {
                resp.getWriter().write("File too large. Maximum allowed is 10 MB.");
                return;
            }

            // Get submitted file name in a safe way
            String submittedFileName = getSubmittedFileNameSafe(filePart);
            if (submittedFileName == null || submittedFileName.trim().isEmpty()) {
                resp.getWriter().write("Uploaded file has no filename.");
                return;
            }

            // Strip any path information - prevents path traversal via filename
            String fileName = Paths.get(submittedFileName).getFileName().toString();

            // Enforce .pdf extension (case-insensitive)
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only .pdf files are allowed.");
                return;
            }

            // Check content type header (helpful but not authoritative)
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.toLowerCase().contains("pdf")) {
                // allow but warn: we choose to reject if content type doesn't mention pdf
                resp.getWriter().write("Content-Type is not PDF.");
                return;
            }

            Path uploadsDir = Paths.get("uploads");
            try {
                // Create uploads directory if it doesn't exist
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Unable to create uploads directory.");
                return;
            }

            // Save incoming stream to a temporary file first to validate contents
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("upload-", ".tmp");
                try (InputStream in = filePart.getInputStream()) {
                    // Copy the uploaded data to temp file
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Validate PDF magic bytes to reduce risk of disguised dangerous files
                try (InputStream tIn = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                    byte[] header = new byte[4];
                    int read = tIn.read(header);
                    if (read < 4 || header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                        // "%PDF" in ASCII -> 0x25 0x50 0x44 0x46
                        Files.deleteIfExists(tempFile);
                        resp.getWriter().write("Uploaded file does not look like a PDF.");
                        return;
                    }
                }

                // Create a unique filename to avoid overwriting existing files
                String uniqueFileName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + "-" + fileName;
                Path target = uploadsDir.resolve(uniqueFileName).normalize();

                // Ensure the resolved target is inside the uploads directory (prevents path traversal)
                Path uploadsDirAbs = uploadsDir.toAbsolutePath().normalize();
                Path targetAbs = target.toAbsolutePath().normalize();
                if (!targetAbs.startsWith(uploadsDirAbs)) {
                    Files.deleteIfExists(tempFile);
                    resp.getWriter().write("Invalid target path.");
                    return;
                }

                // Move the validated temp file to the final uploads directory
                try {
                    Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (UnsupportedOperationException ex) {
                    // ATOMIC_MOVE might not be supported on some filesystems; fallback to non-atomic move
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                }

                resp.getWriter().write("File uploaded successfully as: " + uniqueFileName);
                return;

            } catch (IOException e) {
                // Clean up temp file if something went wrong
                try { if (tempFile != null) Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
                resp.getWriter().write("Server error while saving the file.");
                return;
            }
        }

        // Helper to get submitted filename in a servlet 3.x safe way, with header fallback
        private static String getSubmittedFileNameSafe(Part part) {
            try {
                String fileName = part.getSubmittedFileName();
                if (fileName != null) return fileName;
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                // fallback to parsing content-disposition header
            }
            String cd = part.getHeader("content-disposition");
            if (cd == null) return null;
            for (String token : cd.split(";")) {
                token = token.trim();
                if (token.startsWith("filename=")) {
                    String name = token.substring(9);
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                        name = name.substring(1, name.length() - 1);
                    }
                    return name;
                }
            }
            return null;
        }
    }
}
