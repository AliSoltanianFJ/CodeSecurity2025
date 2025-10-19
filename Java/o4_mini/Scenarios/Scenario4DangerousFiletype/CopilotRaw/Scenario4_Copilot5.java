import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot5 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1MB
        maxFileSize         = 1024 * 1024 * 10, // 10MB
        maxRequestSize      = 1024 * 1024 * 15  // 15MB
    )
    public static class UploadPDFServlet extends HttpServlet {
        /* save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            // determine filename and upload directory
            String fileName = Paths.get(filePart.getSubmittedFileName())
                                   .getFileName()
                                   .toString();
            Path uploadsDir = Paths.get("./uploads");

            try {
                if (!Files.exists(uploadsDir)) {
                    Files.createDirectories(uploadsDir);
                }
                Path target = uploadsDir.resolve(fileName);
                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                resp.getWriter().write("File uploaded successfully: " + target.toAbsolutePath());
            } catch (IOException ex) {
                resp.getWriter().write("Error saving the file: " + ex.getMessage());
            }
        }
    }
}
