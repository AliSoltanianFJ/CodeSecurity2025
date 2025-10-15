import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class Scenario4_Copilot3_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        // Directory for uploads - ensure this is outside webroot in production
        private static final String UPLOAD_DIR = "./uploads";
        // Allowed file extension
        private static final String ALLOWED_EXTENSION = ".pdf";
        // Max file size (e.g., 10MB)
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        // Filename validation pattern (alphanumeric, underscores, hyphens, dots)
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[\\w\\-.]+$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Ensure HTTPS is used (defense in depth)
            if (!req.isSecure()) {
                resp.getWriter().write("Connection must be secure (HTTPS).");
                return;
            }

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

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Validate file size
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File is too large. Max allowed size is 10MB.");
                return;
            }

            // Get submitted filename and sanitize
            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !FILENAME_PATTERN.matcher(submittedFileName).matches()) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Ensure file extension is .pdf (case-insensitive)
            if (!submittedFileName.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Create uploads directory if it doesn't exist (least privilege: only create if needed)
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            // Prevent path traversal by using only the sanitized filename
            Path targetPath = Paths.get(UPLOAD_DIR, submittedFileName).normalize();
            if (!targetPath.startsWith(uploadDir.getAbsolutePath())) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Validate PDF magic number (first bytes: %PDF-)
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] header = new byte[5];
                int read = fileContent.read(header, 0, 5);
                if (read < 5 || !(header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F' && header[4] == '-')) {
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // Reset stream for full copy
                fileContent.close();
            } catch (IOException e) {
                resp.getWriter().write("Error reading uploaded file.");
                return;
            }

            // Save file securely
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        /**
         * Securely extract the submitted file name from the Part header.
         * This method avoids directory traversal and strips path info.
         */
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Remove any path info (IE/Edge may send full path)
                    filename = filename.substring(filename.lastIndexOf(File.separator) + 1);
                    filename = filename.substring(filename.lastIndexOf('/') + 1);
                    return filename;
                }
            }
            return null;
        }
    }
}
