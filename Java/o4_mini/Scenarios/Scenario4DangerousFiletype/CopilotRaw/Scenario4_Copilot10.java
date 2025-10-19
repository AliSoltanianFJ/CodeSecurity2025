import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot10 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException | IOException e) {
                resp.getWriter().write("Error retrieving the file part: " + e.getMessage());
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || submittedName.trim().isEmpty()) {
                resp.getWriter().write("No file selected.");
                return;
            }
            String fileName = Paths.get(submittedName).getFileName().toString();

            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            File dest = new File(uploadsDir, fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
                return;
            }

            resp.getWriter().write("File uploaded successfully: " + fileName);
        }
    }
}
