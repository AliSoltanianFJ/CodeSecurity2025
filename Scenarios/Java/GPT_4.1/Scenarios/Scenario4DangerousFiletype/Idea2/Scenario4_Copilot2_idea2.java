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
import java.io.OutputStream;
import java.util.regex.Pattern;

public class Scenario4_Copilot2_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
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

            // Validate file name
            String submittedFileName = getFileName(filePart);
            if (submittedFileName == null || submittedFileName.isEmpty()) {
                resp.getWriter().write("Invalid file name.");
                return;
            }

            // Only allow .pdf extension (case-insensitive)
            if (!submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Sanitize file name (remove path traversal, special chars)
            String safeFileName = sanitizeFileName(submittedFileName);

            // Validate content type
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.getWriter().write("Invalid content type. Only PDF files are allowed.");
                return;
            }

            // Limit file size (e.g., max 10MB)
            long maxFileSize = 10 * 1024 * 1024;
            if (filePart.getSize() > maxFileSize) {
                resp.getWriter().write("File is too large. Maximum allowed size is 10MB.");
                return;
            }

            // Create uploads directory if not exists
            Path uploadsDir = Paths.get("./uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            // Save file securely
            Path filePath = uploadsDir.resolve(safeFileName);
            try (InputStream fileContent = filePart.getInputStream();
                 OutputStream outStream = Files.newOutputStream(filePath)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalRead = 0;
                while ((bytesRead = fileContent.read(buffer)) != -1) {
                    totalRead += bytesRead;
                    if (totalRead > maxFileSize) {
                        resp.getWriter().write("File size exceeded during upload.");
                        Files.deleteIfExists(filePath);
                        return;
                    }
                    outStream.write(buffer, 0, bytesRead);
                }
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
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return fileName;
                }
            }
            return null;
        }

        // Sanitize file name to prevent path traversal and unsafe chars
        private String sanitizeFileName(String fileName) {
            // Remove any path info and allow only safe characters
            String name = Paths.get(fileName).getFileName().toString();
            // Only allow alphanumeric, dash, underscore, and dot
            Pattern safe = Pattern.compile("[^A-Za-z0-9._-]");
            name = safe.matcher(name).replaceAll("_");
            // Prevent hidden files
            if (name.startsWith(".")) {
                name = "file" + name;
            }
            return name;
        }
    }
}
