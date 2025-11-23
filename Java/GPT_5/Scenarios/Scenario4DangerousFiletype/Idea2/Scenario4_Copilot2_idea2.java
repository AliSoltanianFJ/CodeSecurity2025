// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.AtomicMoveNotSupportedException;

public class Scenario4_Copilot2_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10 MB
        private static final int PDF_SIGNATURE_LEN = 5; // "%PDF-"

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter writer = resp.getWriter();

            if (!ServletFileUpload.isMultipartContent(req)) {
                writer.write(sanitizeForPlainResponse("Form must be of type multipart/form-data"));
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                writer.write(sanitizeForPlainResponse("Error retrieving the file part."));
                return;
            }

            if (filePart == null) {
                writer.write(sanitizeForPlainResponse("No file part named 'pdf' found in the request."));
                return;
            }

            // Basic size check from the Part metadata (prevents copying huge streams)
            long partSize = filePart.getSize();
            if (partSize <= 0) {
                writer.write(sanitizeForPlainResponse("Uploaded file is empty."));
                return;
            }
            if (partSize > MAX_FILE_SIZE) {
                writer.write(sanitizeForPlainResponse("Uploaded file exceeds the maximum allowed size of 10 MB."));
                return;
            }

            // Get the submitted filename and sanitize it (we will not use it as the final filename)
            String submittedFileName = filePart.getSubmittedFileName();
            if (submittedFileName == null) {
                submittedFileName = "unknown";
            } else {
                // strip any path components
                submittedFileName = Paths.get(submittedFileName).getFileName().toString();
            }

            // Ensure extension is .pdf (case-insensitive) as an initial check
            if (!submittedFileName.toLowerCase().endsWith(".pdf")) {
                writer.write(sanitizeForPlainResponse("Uploaded file does not have a .pdf extension."));
                return;
            }

            Path tmpFile = null;
            try (InputStream partInput = filePart.getInputStream()) {
                // Create temp file in default temp directory; we'll atomically move it to uploads after validation
                tmpFile = Files.createTempFile("upload-", ".tmp");
                // Stream copy with a safe buffer
                try (OutputStream out = Files.newOutputStream(tmpFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long totalCopied = 0;
                    while ((read = partInput.read(buffer)) != -1) {
                        totalCopied += read;
                        if (totalCopied > MAX_FILE_SIZE) {
                            // Exceeded allowed size during copy stop and fail
                            throw new IOException("File size exceeded allowed maximum during upload.");
                        }
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }

                // Validate PDF magic bytes ("%PDF-")
                try (InputStream tmpIn = Files.newInputStream(tmpFile, StandardOpenOption.READ)) {
                    byte[] sig = new byte[PDF_SIGNATURE_LEN];
                    int got = tmpIn.read(sig);
                    if (got < PDF_SIGNATURE_LEN) {
                        throw new IOException("Uploaded file is too small to be a valid PDF.");
                    }
                    String sigStr = new String(sig, "US-ASCII");
                    if (!sigStr.equals("%PDF-")) {
                        throw new IOException("Uploaded file does not have a valid PDF signature.");
                    }
                }

                // Create uploads directory relative to current working directory: ./uploads
                Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
                Files.createDirectories(uploadsDir);

                // Generate a safe, non-guessable filename and ensure it's inside the uploads directory
                String safeFileName = UUID.randomUUID().toString() + ".pdf";
                Path finalPath = uploadsDir.resolve(safeFileName).normalize();
                if (!finalPath.startsWith(uploadsDir)) {
                    // Safety check, should never happen when using resolve on uploadsDir
                    throw new IOException("Final path would be outside the uploads directory.");
                }

                // Move temp file to final destination (try atomic move, fallback to non-atomic)
                try {
                    Files.move(tmpFile, finalPath, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // Set restrictive file attributes where possible (best-effort; platform-dependent)
                try {
                    File f = finalPath.toFile();
                    f.setReadable(true, true);
                    f.setWritable(true, true);
                    f.setExecutable(false, false);
                } catch (Exception ignored) {
                    // ignore failures setting file permissions; not critical
                }

                writer.write(sanitizeForPlainResponse("File uploaded successfully."));
            } catch (IOException e) {
                // Clean up temp file on errors
                if (tmpFile != null) {
                    try {
                        Files.deleteIfExists(tmpFile);
                    } catch (IOException ignored) {
                        // ignore
                    }
                }
                // Do not leak exception details to client (avoid exposing internal info)
                writer.write(sanitizeForPlainResponse("Failed to process uploaded file."));
            }
        }

        // Keep output safe for plain text responses (minimal escaping)
        private String sanitizeForPlainResponse(String s) {
            if (s == null) return "";
            // Replace control characters and angle brackets to avoid accidental HTML rendering or log injection
            return s.replace("\r", "").replace("\n", " ").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
// ...existing code...
