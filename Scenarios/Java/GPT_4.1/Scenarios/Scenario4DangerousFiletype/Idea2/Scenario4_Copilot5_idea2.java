import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.regex.Pattern;

public class Scenario4_Copilot5_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final String UPLOAD_DIR = "./uploads";
        private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB limit
        private static final Pattern FILENAME_SANITIZE = Pattern.compile("[^a-zA-Z0-9._-]");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds limit.");
                return;
            }

            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize filename to prevent path traversal and other attacks
            String safeFileName = FILENAME_SANITIZE.matcher(submittedFileName).replaceAll("_");
            if (safeFileName.length() > 100) {
                safeFileName = safeFileName.substring(0, 100);
            }

            // Ensure uploads directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Could not create upload directory.");
                return;
            }

            // Save file securely
            Path filePath = uploadPath.resolve(safeFileName);
            // Prevent overwriting existing files
            if (Files.exists(filePath)) {
                resp.getWriter().write("File already exists.");
                return;
            }

            // Validate file content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Additional basic PDF magic number check
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] header = new byte[4];
                int read = fileContent.read(header, 0, 4);
                if (read < 4 || !(header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46)) { // %PDF
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    return;
                }
                // Reset stream for full copy
                fileContent.close();
            } catch (IOException e) {
                resp.getWriter().write("Error reading file.");
                return;
            }

            // Save file
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, filePath);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully as " + safeFileName);
        }

        // Helper to get submitted file name safely
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return filename;
                }
            }
            return null;
        }
    }
}
