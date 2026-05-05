import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;

public class Scenario4_Copilot1 {

    @MultipartConfig
    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error retrieving the file part: " + e.getMessage());
                return;
            }

            if (filePart == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file part named 'pdf' found in the request.");
                return;
            }

            String submittedName = null;
            try {
                submittedName = filePart.getSubmittedFileName();
            } catch (Exception ex) {
                // getSubmittedFileName may not be supported in some containers; fall back to null
                submittedName = null;
            }

            // Basic filename sanitization: keep only file name component
            String fileName = (submittedName != null) ? Paths.get(submittedName).getFileName().toString() : null;

            // Validate content type or filename extension
            String contentType = filePart.getContentType();
            boolean looksLikePdf = false;
            if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                looksLikePdf = true;
            }
            if (!looksLikePdf && fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                looksLikePdf = true;
            }

            if (!looksLikePdf) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Uploaded file does not appear to be a PDF.");
                return;
            }

            // Ensure uploads directory exists (relative to working directory)
            Path uploadsDir = Paths.get("uploads");
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to create uploads directory: " + e.getMessage());
                return;
            }

            // If filename is missing, generate a safe one
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "upload-" + System.currentTimeMillis() + ".pdf";
            } else {
                // Remove any path separator characters for extra safety
                fileName = fileName.replaceAll("[\\\\/]+", "_");
            }

            Path target = uploadsDir.resolve(fileName);

            // Save the file
            try (InputStream in = filePart.getInputStream()) {
                // Limit size implicitly by the container or handle here if needed
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving uploaded file: " + e.getMessage());
                return;
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("File uploaded successfully to: " + target.toString());
        }
    }
}
