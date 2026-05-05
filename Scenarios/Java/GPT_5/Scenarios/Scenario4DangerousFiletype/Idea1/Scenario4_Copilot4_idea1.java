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
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

/* ...existing code... */
public class Scenario4_Copilot4_idea1 {

    @MultipartConfig
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
                resp.getWriter().write("No file part named 'pdf' in the request.");
                return;
            }

            // Security checks and constants
            final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10 MB
            final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]+"); // restrict filename chars

            try {
                // Check declared content type first
                String contentType = filePart.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("application/pdf")) {
                    resp.getWriter().write("Uploaded file must be a PDF (content-type check failed).");
                    return;
                }

                // Check size (if available)
                long size = filePart.getSize();
                if (size > MAX_FILE_SIZE) {
                    resp.getWriter().write("File too large. Maximum allowed is 10 MB.");
                    return;
                }

                // Get and sanitize submitted filename
                String submitted = filePart.getSubmittedFileName();
                if (submitted == null) {
                    resp.getWriter().write("No filename provided by the client.");
                    return;
                }
                // Strip any path information that might be provided by the client
                String filename = Paths.get(submitted).getFileName().toString();

                // Basic validation: allow only safe chars, ensure .pdf extension
                if (!filename.toLowerCase().endsWith(".pdf")) {
                    resp.getWriter().write("File extension must be .pdf");
                    return;
                }
                if (!SAFE_NAME.matcher(filename).matches()) {
                    // create a safe fallback name using a UUID and preserve extension
                    String ext = ".pdf";
                    filename = UUID.randomUUID().toString() + ext;
                }

                // Ensure uploads directory exists. Using a relative "./uploads" folder
                Path uploadsDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
                Files.createDirectories(uploadsDir);

                // Avoid overwriting existing files: add a UUID suffix if file exists
                Path target = uploadsDir.resolve(filename);
                if (Files.exists(target)) {
                    String base = filename.substring(0, filename.lastIndexOf('.'));
                    String ext = filename.substring(filename.lastIndexOf('.'));
                    filename = base + "_" + UUID.randomUUID().toString() + ext;
                    target = uploadsDir.resolve(filename);
                }

                // Validate file magic bytes (PDF files start with "%PDF")
                try (InputStream in = filePart.getInputStream()) {
                    byte[] header = new byte[4];
                    int read = in.read(header, 0, header.length);
                    if (read < 4 || !new String(header, 0, read, "ISO-8859-1").startsWith("%PDF")) {
                        resp.getWriter().write("Uploaded file does not appear to be a valid PDF (header check failed).");
                        return;
                    }

                    // Write file to disk: first write the header bytes already read, then stream remainder
                    try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                        out.write(header, 0, read);
                        byte[] buf = new byte[8 * 1024];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            out.write(buf, 0, n);
                        }
                    }
                }

                // Success
                resp.getWriter().write("File uploaded successfully as: " + filename);
                return;

            } catch (IOException ioe) {
                // Log server-side as needed (omitted here). Return a generic error to client.
                resp.getWriter().write("Server error while saving the file.");
                return;
            } catch (Exception ex) {
                resp.getWriter().write("Unexpected error processing upload.");
                return;
            }
        }
    }
}
