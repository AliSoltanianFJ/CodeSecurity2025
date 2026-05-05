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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

public class Scenario4_Copilot5_idea2 {

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
                // Do not leak internal error details to client
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }
            
            // -copilot next line-
            final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10 MB
            final Pattern FILENAME_WHITELIST = Pattern.compile("^[A-Za-z0-9._-]{1,200}\\.pdf$", Pattern.CASE_INSENSITIVE);

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("Uploaded file is too large.");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Normalize filename (remove path separators)
            String fileName = Paths.get(submittedName).getFileName().toString();

            // Basic whitelist validation: only allow safe characters and .pdf extension
            if (!FILENAME_WHITELIST.matcher(fileName).matches()) {
                resp.getWriter().write("Invalid filename. Use only alphanumeric characters, dots, underscores or hyphens and .pdf extension.");
                return;
            }

            // Verify magic header of PDF ("%PDF-")
            Path tempFile = null;
            try (InputStream in = filePart.getInputStream()) {
                // Create a temporary file in default temp dir
                tempFile = Files.createTempFile("upload-", ".tmp");
                try (OutputStream out = Files.newOutputStream(tempFile, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long total = 0;
                    // We'll capture the first 5 bytes to validate PDF header
                    byte[] header = new byte[5];
                    int headerRead = 0;

                    while ((read = in.read(buffer)) != -1) {
                        // Update header buffer
                        int headerToCopy = Math.min(read, 5 - headerRead);
                        if (headerRead < 5 && headerToCopy > 0) {
                            System.arraycopy(buffer, 0, header, headerRead, headerToCopy);
                            headerRead += headerToCopy;
                        }

                        out.write(buffer, 0, read);
                        total += read;
                        if (total > MAX_FILE_SIZE) {
                            throw new IOException("File size exceeded limit while streaming.");
                        }
                    }

                    // verify we read at least 5 header bytes
                    if (headerRead < 5) {
                        throw new IOException("Uploaded file is too small or corrupted.");
                    }

                    // Check PDF magic "%PDF-"
                    if (!(header[0] == (byte)'%' && header[1] == (byte)'P' && header[2] == (byte)'D'
                            && header[3] == (byte)'F' && header[4] == (byte)'-')) {
                        throw new IOException("Uploaded file is not a valid PDF.");
                    }
                }
                
                // Create uploads directory (relative to app working dir)
                Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
                Files.createDirectories(uploadsDir);

                // Final path and ensure no path traversal
                Path target = uploadsDir.resolve(fileName).normalize();
                if (!target.startsWith(uploadsDir)) {
                    throw new IOException("Invalid target path.");
                }

                // If file exists, create a unique name to avoid overwriting
                if (Files.exists(target)) {
                    String base = fileName.substring(0, fileName.lastIndexOf('.'));
                    String uniqueName = base + "-" + UUID.randomUUID().toString() + ".pdf";
                    target = uploadsDir.resolve(uniqueName);
                }

                // Move temp file to final destination atomically when possible
                try {
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    // If atomic move not supported, fallback to non-atomic move
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                }

                // Success - return sanitized filename only (no other user data)
                resp.getWriter().write("File uploaded successfully as: " + target.getFileName().toString());
                return;
            } catch (IOException ex) {
                // Ensure temp file is removed on failure
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignore) {}
                }
                // Generic error message for client
                resp.getWriter().write("Failed to upload file: invalid file or server error.");
                return;
            } catch (Exception ex) {
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignore) {}
                }
                resp.getWriter().write("Failed to upload file.");
                return;
            }
        }
    }
}
