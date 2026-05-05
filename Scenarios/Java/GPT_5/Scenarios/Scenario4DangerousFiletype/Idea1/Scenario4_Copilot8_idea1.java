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
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/*
  Saves uploaded PDFs to ./uploads.
  Security measures included:
  - Checks multipart/form-data request.
  - Limits upload size.
  - Sanitizes submitted filename (removes path-separators and unsafe chars).
  - Verifies PDF magic bytes ("%PDF-") to avoid dangerous filetypes disguised as .pdf.
  - Uses a UUID-prefixed filename to avoid collisions and predictable paths.
  - Creates (and normalizes) uploads directory to avoid directory traversal.
*/
public class Scenario4_Copilot8_idea1 {

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
                resp.getWriter().write("No file part named 'pdf' found in request.");
                return;
            }

            // Enforce a maximum file size (e.g. 10 MB) to limit resource usage
            final long MAX_SIZE = 10L * 1024L * 1024L;
            if (filePart.getSize() > MAX_SIZE) {
                resp.getWriter().write("Uploaded file is too large.");
                return;
            }

            // Obtain submitted file name and sanitize it to prevent directory traversal and illegal chars
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || submittedName.trim().isEmpty()) {
                submittedName = "upload.pdf";
            }
            // Keep only the base name and remove potentially dangerous characters
            String sanitized = Paths.get(submittedName).getFileName().toString();
            sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
            // Ensure filename ends with .pdf
            if (!sanitized.toLowerCase().endsWith(".pdf")) {
                sanitized = sanitized + ".pdf";
            }

            // Read the uploaded data into memory (safe because we enforce MAX_SIZE above)
            byte[] data;
            try (InputStream is = filePart.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                long total = 0;
                while ((read = is.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_SIZE) { // double-check while streaming
                        resp.getWriter().write("Uploaded file exceeds size limit.");
                        return;
                    }
                    baos.write(buffer, 0, read);
                }
                data = baos.toByteArray();
            } catch (IOException e) {
                resp.getWriter().write("Error reading uploaded file.");
                return;
            }

            // Verify PDF magic bytes ("%PDF-") to avoid accepting arbitrary file types with .pdf extension
            if (data.length < 4 || !(data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F')) {
                resp.getWriter().write("Uploaded file is not a valid PDF.");
                return;
            }

            // Prepare uploads directory (./uploads) and normalize path to avoid traversal
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            // Create a unique filename to avoid collisions and predictable paths
            String uniqueName = UUID.randomUUID().toString() + "_" + sanitized;
            Path target = uploadsDir.resolve(uniqueName).normalize();

            // Ensure the target path is inside the uploads directory (defense-in-depth)
            if (!target.startsWith(uploadsDir)) {
                resp.getWriter().write("Invalid target path.");
                return;
            }

            // Write file atomically
            try {
                Files.write(target, data, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                resp.getWriter().write("Failed to save uploaded file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully as " + uniqueName);
        }
    }
}
// ...existing code...
