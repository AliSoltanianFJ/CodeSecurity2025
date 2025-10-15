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

public class Scenario4_Copilot8_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory for uploads. Should be outside webroot in production.
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension pattern (only .pdf)
        private static final Pattern PDF_FILE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+\\.pdf$");
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

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
            } catch (IOException e) {
                resp.getWriter().write("IO error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds limit.");
                return;
            }

            // Get submitted file name securely
            String submittedFileName = getFileName(filePart);
            if (submittedFileName == null || !PDF_FILE_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name or file type. Only .pdf files are allowed.");
                return;
            }

            // Check content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Invalid content type. Only PDF files are allowed.");
                return;
            }

            // Ensure uploads directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to create upload directory.");
                return;
            }

            // Prevent path traversal by normalizing file name
            Path targetFile = uploadDir.resolve(Paths.get(submittedFileName)).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Save file securely
            try {
                // Overwrite if file exists (can be changed to fail if exists)
                Files.copy(filePart.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Server error: Unable to save file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Securely extract file name from Part header.
         * Prevents header injection and path traversal.
         */
        private String getFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                cd = cd.trim();
                if (cd.startsWith("filename")) {
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path information
                    fileName = Paths.get(fileName).getFileName().toString();
                    return fileName;
                }
            }
            return null;
        }
    }
}
