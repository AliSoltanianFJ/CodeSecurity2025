import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_3 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
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

            if (filePart == null) {
                resp.getWriter().write("No file selected or error in retrieving the file part.");
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            if (fileName == null || fileName.isEmpty()) {
                resp.getWriter().write("No file selected.");
                return;
            }

            // Create uploads directory if it doesn't exist
            File uploadDir = new File("./uploads");
            if (!uploadDir.exists()) {
                boolean dirCreated = uploadDir.mkdirs();
                if (!dirCreated) {
                    resp.getWriter().write("Failed to create uploads directory.");
                    return;
                }
            }

            // Save the file
            File outputFile = new File(uploadDir, fileName);
            try (InputStream inputStream = filePart.getInputStream()) {
                Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                resp.getWriter().write("File " + fileName + " has been uploaded successfully.");
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}

