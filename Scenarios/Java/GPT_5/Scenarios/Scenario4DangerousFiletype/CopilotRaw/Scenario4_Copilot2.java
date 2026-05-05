// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.util.UUID;

public class Scenario4_Copilot2 {

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
            // save uploaded PDF to ./uploads
            if (filePart == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file part named 'pdf' was provided.");
                return;
            }

            long size = filePart.getSize();
            if (size <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Uploaded file is empty.");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                submittedName = "unnamed.pdf";
            }

            // Basic filename sanitization: keep only the last path element
            String sanitized = Paths.get(submittedName).getFileName().toString();

            String contentType = filePart.getContentType();
            boolean looksLikePdf = false;
            if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                looksLikePdf = true;
            } else if (sanitized.toLowerCase().endsWith(".pdf")) {
                // fallback check by extension
                looksLikePdf = true;
            }

            if (!looksLikePdf) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF files are accepted.");
                return;
            }

            Path uploadsDir = Paths.get("uploads");
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to create uploads directory: " + e.getMessage());
                return;
            }

            // Avoid filename collisions by prefixing a UUID
            String savedName = UUID.randomUUID().toString() + "_" + sanitized;
            Path target = uploadsDir.resolve(savedName);

            try (InputStream in = filePart.getInputStream()) {
                // Use REPLACE_EXISTING to be explicit; collisions are unlikely due to UUID prefix
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to save uploaded file: " + e.getMessage());
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded successfully as: " + target.toString());
        }
    }
}
