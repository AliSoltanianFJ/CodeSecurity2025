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
import java.io.InputStream;
import java.util.regex.Pattern;

public class Scenario4_Copilot10_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory to save uploads
        private static final String UPLOAD_DIR = "./uploads";
        // Only allow .pdf files, case-insensitive
        private static final Pattern PDF_FILE_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+\\.pdf$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

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

            // Validate filename
            String submittedFileName = getFileName(filePart);
            if (submittedFileName == null || !PDF_FILE_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name. Only .pdf files are allowed.");
                return;
            }

            // Limit file size to 10MB
            long maxFileSize = 10 * 1024 * 1024;
            if (filePart.getSize() > maxFileSize) {
                resp.getWriter().write("File size exceeds limit (10MB).");
                return;
            }

            // Ensure uploads directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }

            // Prevent path traversal
            Path filePath = uploadPath.resolve(submittedFileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Check file content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Uploaded file is not a PDF.");
                return;
            }

            // Save file securely
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, filePath);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        // Helper to get submitted file name safely
        private String getFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path info (IE/Edge may send full path)
                    filename = Paths.get(filename).getFileName().toString();
                    return filename;
                }
            }
            return null;
        }
    }
}
