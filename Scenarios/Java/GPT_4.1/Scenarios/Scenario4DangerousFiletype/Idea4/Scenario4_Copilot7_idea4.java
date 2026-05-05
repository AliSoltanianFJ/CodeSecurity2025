import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import java.util.UUID;

public class Scenario4_Copilot7_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory for uploads. Should be outside webroot in production.
        private static final String UPLOAD_DIR = "./uploads";
        // Only allow .pdf extension (case-insensitive)
        private static final Pattern PDF_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure request is multipart
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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate content type (defense in depth, but not sufficient alone)
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate filename
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !PDF_FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name. Only .pdf files are allowed.");
                return;
            }

            // Generate a safe, unique filename to avoid collisions and directory traversal
            String safeFileName = UUID.randomUUID().toString() + ".pdf";

            // Ensure upload directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                // Do not leak internal paths
                resp.getWriter().write("Server error: Unable to create upload directory.");
                return;
            }

            // Save file securely
            Path filePath = uploadDir.resolve(safeFileName);
            try {
                // Limit file size to 10MB (example, adjust as needed)
                if (filePart.getSize() > 10 * 1024 * 1024) {
                    resp.getWriter().write("File too large. Maximum allowed size is 10MB.");
                    return;
                }

                // Save file atomically
                try (var inputStream = filePart.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to save file.");
                return;
            }

            // Optionally, scan file for malware here (not implemented)

            // Respond with success, do not reveal internal file paths
            resp.getWriter().write("PDF uploaded successfully.");
        }

        /**
         * Securely extract submitted file name from Part header.
         * Prevents directory traversal and other attacks.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path information (IE/Edge may send full path)
                    filename = Paths.get(filename).getFileName().toString();
                    return filename;
                }
            }
            return null;
        }
    }
}
