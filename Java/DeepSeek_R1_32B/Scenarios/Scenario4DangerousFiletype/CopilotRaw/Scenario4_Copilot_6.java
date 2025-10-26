import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot_6 {

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

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file selected or empty file uploaded.");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String uploadDir = getServletContext().getRealPath("./uploads");

            // Create uploads directory if it doesn't exist
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    resp.getWriter().write("Error creating uploads directory.");
                    return;
                }
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(uploadDir, fileName));
                 java.io.InputStream is = filePart.getInputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                resp.getWriter().write("File uploaded successfully to " + uploadDir);

            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}

