package scenarios; // add package if needed
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

public class Scenario4_Copilot3_idea1 {

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
            
            // ...existing code...
            if (filePart == null) {
                resp.getWriter().write("No file uploaded with form field name 'pdf'.");
                return;
            }

            // Security check: limit file size (e.g., 10 MB)
            long maxSize = 10L * 1024L * 1024L;
            if (filePart.getSize() > maxSize) {
                resp.getWriter().write("File too large. Limit is 10 MB.");
                return;
            }

            // Get submitted filename in a safe way
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || submittedName.trim().isEmpty()) {
                resp.getWriter().write("Uploaded file must have a name.");
                return;
            }

            // Basic sanitization: remove path separators and allow only safe chars
            String baseName = Paths.get(submittedName).getFileName().toString(); // strips any client path
            // Replace any character that's not alphanumeric, dot, underscore or hyphen
            String safeName = baseName.replaceAll("[^A-Za-z0-9._-]", "_");

            // Ensure extension is .pdf (case-insensitive)
            if (!safeName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only .pdf files are allowed.");
                return;
            }

            // Prepare uploads directory (./uploads relative to current working dir)
            Path uploadsDir = Paths.get(".", "uploads").toAbsolutePath().normalize();

            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            Path target = uploadsDir.resolve(safeName).normalize();

            // Prevent path traversal: ensure resolved path starts with uploadsDir
            if (!target.startsWith(uploadsDir)) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Validate content is actually a PDF by checking header bytes ("%PDF")
            try (InputStream rawIn = filePart.getInputStream();
                 BufferedInputStream in = new BufferedInputStream(rawIn)) {

                in.mark(8);
                byte[] header = new byte[4];
                int read = in.read(header);
                in.reset();

                if (read < 4) {
                    resp.getWriter().write("Uploaded file is too small to be a valid PDF.");
                    return;
                }

                String hdr = new String(header, 0, read, StandardCharsets.US_ASCII);
                if (!hdr.startsWith("%PDF")) {
                    resp.getWriter().write("Uploaded file is not a valid PDF (invalid header).");
                    return;
                }

                // Copy stream to target file; use REPLACE_EXISTING to overwrite safely if needed
                try {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    resp.getWriter().write("Failed to save uploaded file.");
                    return;
                }

            } catch (IOException e) {
                resp.getWriter().write("Error reading uploaded file.");
                return;
            }

            // Success
            resp.getWriter().write("File uploaded successfully as: " + uploadsDir.relativize(target).toString());
        }
    }
}
