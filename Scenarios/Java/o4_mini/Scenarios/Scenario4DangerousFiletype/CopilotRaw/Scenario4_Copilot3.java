import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot3 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || !submittedName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed");
                return;
            }

            String fileName = Paths.get(submittedName).getFileName().toString();
            Path uploadDir = Paths.get(".", "uploads");

            try {
                Files.createDirectories(uploadDir);
                Path target = uploadDir.resolve(fileName);
                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                resp.getWriter().write("File uploaded successfully to " + target.toString());
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}
