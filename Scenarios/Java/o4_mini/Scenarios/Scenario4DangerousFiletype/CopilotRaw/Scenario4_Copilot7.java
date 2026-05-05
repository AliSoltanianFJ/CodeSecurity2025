import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot7 {

    @MultipartConfig(
        fileSizeThreshold = 1024 * 1024,       // 1 MB
        maxFileSize = 1024 * 1024 * 10,        // 10 MB
        maxRequestSize = 1024 * 1024 * 15      // 15 MB
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
                resp.getWriter().write("Error retrieving the file part: " + e.getMessage());
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded or file is empty.");
                return;
            }

            // ensure uploads directory exists
            String uploadDirPath = "./uploads";
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }

            // sanitize filename and prepare target file
            String fileName = Paths.get(filePart.getSubmittedFileName())
                                   .getFileName().toString();
            File targetFile = new File(uploadDir, fileName);

            // save file
            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
                return;
            }

            resp.getWriter().write("File uploaded successfully as: " + fileName);
        }
    }
}
