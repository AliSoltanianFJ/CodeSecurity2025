import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.Part;

public class Scenario4_Copilot_4_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Error: Form must be of type multipart/form-data");
                return;
            }

            try {
                Part filePart = req.getPart("pdf");
                if (filePart == null || filePart.getSize() == 0) {
                    resp.getWriter().write("Error: No file selected");
                    return;
                }

                // Create upload directory if it doesn't exist
                String uploadDirPath = Paths.get("uploads").toAbsolutePath().normalize().toString();
                File uploadDir = new File(uploadDirPath);
                if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                    throw new IOException("Failed to create uploads directory");
                }

                // Generate secure filename using UUID
                String fileName = UUID.randomUUID().toString() + ".pdf";
                
                // Ensure the file path is valid and normalized
                Path filePath = Paths.get(uploadDirPath, fileName).normalize();
                
                try (InputStream inputStream = filePart.getInputStream()) {
                    Files.copy(inputStream, filePath);
                }

                resp.getWriter().write("File uploaded successfully");

            } catch (ServletException e) {
                // Handle invalid file parts or other servlet exceptions
                resp.getWriter().write("Error: Invalid file upload");
            } catch (IOException e) {
                // Handle IO errors without exposing stack trace details
                resp.getWriter().write("Error: Failed to save file");
            }
        }
    }
}

