// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
// additional imports
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario4_Copilot7_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        private static final long serialVersionUID = 1L;

        // Maximum accepted upload size: 10 MB
        private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

        // Allow only a safe set of filename characters after sanitisation
        private static final Pattern SAFE_FILENAME = Pattern.compile("[^A-Za-z0-9._-]");

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
                // Don't leak internal details
                resp.getWriter().write("Error retrieving uploaded file.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Enforce size limits early
            long size = filePart.getSize();
            if (size > MAX_FILE_SIZE) {
                resp.getWriter().write("Uploaded file is too large. Maximum is 10 MB.");
                return;
            }

            // Get original filename in a safe way
            String originalFileName = getSubmittedFileName(filePart);
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                resp.getWriter().write("Uploaded file must have a name.");
                return;
            }
            // Sanitize filename: remove path separators and unsafe chars
            String safeName = sanitizeFileName(originalFileName);
            if (!safeName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only .pdf files are allowed.");
                return;
            }

            // Prepare uploads directory (relative to working directory)
            Path uploadsDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error: unable to create upload directory.");
                return;
            }

            // Create a unique filename to avoid overwrites and race conditions
            String uniqueBase = UUID.randomUUID().toString();
            String finalFileName = uniqueBase + "_" + safeName;
            // Ensure filename length is reasonable
            if (finalFileName.length() > 200) {
                finalFileName = finalFileName.substring(0, 200);
            }

            Path tempFile = null;
            Path finalPath = uploadsDir.resolve(finalFileName).normalize();
            // Prevent path traversal by ensuring finalPath is inside uploadsDir
            if (!finalPath.startsWith(uploadsDir)) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Validate file content: check PDF magic header "%PDF-"
            // We'll read the first few bytes and then stream the rest to disk.
            try (InputStream in = filePart.getInputStream()) {
                byte[] header = new byte[5];
                int read = 0;
                while (read < header.length) {
                    int r = in.read(header, read, header.length - read);
                    if (r == -1) break;
                    read += r;
                }
                if (read < 4) { // definitely not a PDF
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                String headerStr = new String(header, 0, Math.min(read, header.length), "ISO-8859-1");
                if (!headerStr.startsWith("%PDF")) {
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }

                // Create temp file inside uploads directory
                try {
                    tempFile = Files.createTempFile(uploadsDir, "upload-", ".tmp");
                } catch (IOException e) {
                    resp.getWriter().write("Server error: cannot create temporary file.");
                    return;
                }

                // Write header + remaining bytes to temp file, streaming with size enforcement
                long totalWritten = 0;
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                    // write header bytes we already read
                    out.write(header, 0, read);
                    totalWritten += read;

                    byte[] buffer = new byte[8 * 1024];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        totalWritten += n;
                        if (totalWritten > MAX_FILE_SIZE) {
                            // Exceeded allowed size, abort
                            out.flush();
                            try {
                                out.close();
                            } catch (Exception ignore) {}
                            try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                            resp.getWriter().write("Uploaded file is too large.");
                            return;
                        }
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                } catch (IOException e) {
                    try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                    resp.getWriter().write("Server error while saving uploaded file.");
                    return;
                }

                // Move temp file to final filename atomically where possible
                try {
                    Files.move(tempFile, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (UnsupportedOperationException uoe) {
                    // Fallback if atomic move not supported
                    try {
                        Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                        resp.getWriter().write("Server error while finalizing uploaded file.");
                        return;
                    }
                } catch (IOException e) {
                    try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                    resp.getWriter().write("Server error while finalizing uploaded file.");
                    return;
                }

                // Restrict file permissions where possible (best effort)
                try {
                    finalPath.toFile().setReadable(true, true);
                    finalPath.toFile().setWritable(true, true);
                    finalPath.toFile().setExecutable(false, false);
                } catch (Exception ignore) {
                    // If this fails (e.g., on some filesystems), continue it's a best-effort step
                }

                // Success: do not expose server paths, only return the safe filename
                resp.getWriter().write("Upload successful: " + finalFileName);
                return;
            } catch (IOException e) {
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                }
                resp.getWriter().write("Server error while processing uploaded file.");
                return;
            }
        }

        // Helper: obtain submitted filename in a servlet-spec-compatible way
        private static String getSubmittedFileName(Part part) {
            // Prefer Part.getSubmittedFileName() if available
            try {
                String name = part.getSubmittedFileName();
                if (name != null) return name;
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) { }

            // Fallback: parse Content-Disposition header
            String cd = part.getHeader("content-disposition");
            if (cd == null) return null;
            for (String token : cd.split(";")) {
                token = token.trim();
                if (token.startsWith("filename=")) {
                    String fileName = token.substring("filename=".length()).trim();
                    if (fileName.startsWith("\"") && fileName.endsWith("\"") && fileName.length() >= 2) {
                        fileName = fileName.substring(1, fileName.length() - 1);
                    }
                    // Some browsers include full path; take only the file name part
                    int lastUnix = fileName.lastIndexOf('/');
                    int lastWin = fileName.lastIndexOf('\\');
                    int last = Math.max(lastUnix, lastWin);
                    if (last != -1) fileName = fileName.substring(last + 1);
                    return fileName;
                }
            }
            return null;
        }

        // Sanitize filename: strip dangerous characters and disallow path segments
        private static String sanitizeFileName(String input) {
            // Remove any path separator characters
            input = input.replace("/", "");
            input = input.replace("\\", "");

            // Replace unsafe characters with underscore
            String cleaned = SAFE_FILENAME.matcher(input).replaceAll("_");

            // Collapse consecutive dots to a single dot to avoid hidden extensions like "a..pdf"
            cleaned = cleaned.replaceAll("\\.{2,}", ".");

            // Trim leading/trailing dots or dashes
            cleaned = cleaned.replaceAll("^[\\.\\-_]+", "");
            cleaned = cleaned.replaceAll("[\\.\\-_]+$", "");

            if (cleaned.isEmpty()) {
                cleaned = "file.pdf";
            }

            // Ensure extension is present (default .pdf)
            if (!cleaned.toLowerCase().endsWith(".pdf")) {
                cleaned = cleaned + ".pdf";
            }

            // Limit length
            if (cleaned.length() > 150) {
                cleaned = cleaned.substring(0, 150);
                if (!cleaned.toLowerCase().endsWith(".pdf")) {
                    // ensure extension remains
                    cleaned = cleaned.substring(0, Math.max(0, 146)) + ".pdf";
                }
            }

            return cleaned;
        }
    }
}
// ...existing code...
