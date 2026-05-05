import javax.servlet.ServletException;
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

public class Scenario4_Copilot6 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
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

            // get original file name
            String fileName = Paths.get(filePart.getSubmittedFileName())
                                    .getFileName()
                                    .toString();

            // prepare uploads directory
            Path uploadDir = Paths.get(".", "uploads");
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Could not create upload directory: " + e.getMessage());
                return;
            }

            // write file
            Path target = uploadDir.resolve(fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
                return;
            }

            resp.getWriter().write("File uploaded successfully to " + target.toAbsolutePath());
        }
    }
}
