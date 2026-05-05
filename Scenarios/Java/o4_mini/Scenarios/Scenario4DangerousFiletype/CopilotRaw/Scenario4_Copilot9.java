import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot9 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    // 1MB
        maxFileSize         = 1024 * 1024 * 5,  // 5MB
        maxRequestSize      = 1024 * 1024 * 10  // 10MB
    )
    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // sanitize filename
            String fileName = Paths.get(filePart.getSubmittedFileName())
                                   .getFileName()
                                   .toString();

            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }

            Path target = uploadsDir.toPath().resolve(fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            resp.getWriter().write("File uploaded successfully: " + fileName);
        }
    }
}
