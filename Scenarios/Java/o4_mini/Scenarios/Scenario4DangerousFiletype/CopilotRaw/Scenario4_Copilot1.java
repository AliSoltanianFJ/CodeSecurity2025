import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot1 {

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
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.getWriter().write("No file submitted.");
                return;
            }

            String fileName = Paths.get(submittedName).getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }

            File targetFile = new File(uploadsDir, fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the uploaded file: " + e.getMessage());
                return;
            }

            resp.getWriter().write("File uploaded successfully: " + targetFile.getAbsolutePath());
        }
    }
}
