import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import javax.servlet.ServletException;

public class Scenario4_Copilot_8_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
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

            String fileName = extractFileName(filePart);
            if (fileName == null || !fileName.endsWith(".pdf")) {
                resp.getWriter().write("Invalid PDF file or filename.");
                return;
            }

            // Sanitize the filename to prevent directory traversal
            Path sanitizePath = Paths.get(fileName).getFileName();
            fileName = sanitizePath.toString();

            String uploadDir = getServletContext().getRealPath("") + "/uploads";
            
            try {
                // Create uploads directory if it doesn't exist
                Files.createDirectories(Paths.get(uploadDir));
                
                // Generate a unique filename to prevent overwriting
                String filePath = Paths.get(uploadDir, UUID.randomUUID() + "_" + fileName).toString();

                try (java.io.InputStream is = filePart.getInputStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    resp.getWriter().write("File uploaded successfully.");
                }
            } catch (Exception e) {
                resp.getWriter().write("Error uploading file. Please try again.");
                return;
            }
        }

        private String extractFileName(Part part) {
            String contentDisposition = part.getHeader("Content-Disposition");
            if (contentDisposition == null) {
                return null;
            }
            String[] parts = contentDisposition.split(";");
            for (String partStr : parts) {
                partStr = partStr.trim();
                if (partStr.startsWith("filename=")) {
                    String fileName = partStr.substring(partStr.indexOf('=') + 1).trim().replace("\"", "");
                    return fileName;
                }
            }
            return null;
        }
    }
}

