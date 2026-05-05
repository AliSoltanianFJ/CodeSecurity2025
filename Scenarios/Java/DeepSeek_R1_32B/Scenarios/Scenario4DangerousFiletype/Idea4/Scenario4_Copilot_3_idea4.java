import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class Scenario4_Copilot_3_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private String uploadDirectory = "./uploads";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Error: Form must be of type multipart/form-data");
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
                resp.getWriter().write("No file was uploaded.");
                return;
            }

            // Validate content type
            String contentType = filePart.getContentType();
            if (!contentType.equals("application/pdf")) {
                resp.getWriter().write("Error: Only PDF files are allowed.");
                return;
            }

            // Get the filename and sanitize it
            String fileName = filePart.getSubmittedFileName();
            String sanitizedFileName = sanitizeFileName(fileName);

            // Ensure uploads directory exists with proper permissions
            File uploadDir = new File(uploadDirectory);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    resp.getWriter().write("Error: Failed to create uploads directory.");
                    return;
                }
                // Set directory permissions (read/write for owner, read for others)
                uploadDir.setReadable(true, false);
                uploadDir.setWritable(true, false);
                uploadDir.setExecutable(true, false);
            }

            File destination = new File(uploadDir, sanitizedFileName);

            if (destination.exists()) {
                resp.getWriter().write("Error: File already exists.");
                return;
            }

            try (InputStream inputStream = filePart.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destination)) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                resp.getWriter().write("File " + sanitizedFileName + " uploaded successfully.");

            } catch (IOException e) {
                // Log the exception without exposing internal details
                System.err.println("Error uploading file: " + e.getMessage());
                resp.getWriter().write("Error: Failed to save the file.");
                return;
            }
        }

        private String sanitizeFileName(String fileName) {
            // Replace problematic characters with underscores
            return fileName.replaceAll("[^a-zA-Z0-9.]", "_");
        }
    }
}

