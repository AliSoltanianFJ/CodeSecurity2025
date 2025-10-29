import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot_10_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            try {
                String uploadDir = getServletContext().getRealPath("./uploads");
                Path uploadPath = Paths.get(uploadDir);

                // Create uploads directory if it doesn't exist
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Generate a unique filename for the uploaded file
                String fileName = UUID.randomUUID().toString() + ".pdf";
                Path filePath = uploadPath.resolve(fileName);

                Part filePart = req.getPart("pdf");

                try (java.io.InputStream is = filePart.getInputStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath.toFile())) {
                    is.transferTo(fos);
                }

                resp.getWriter().write("File uploaded successfully.");

            } catch (Exception e) {
                // Log the error for debugging purposes
                System.err.println("Error uploading file: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error uploading file.");
            }
        }

        private boolean isMultipartContent(HttpServletRequest request) {
            return ServletFileUpload.isMultipartContent(request);
        }
    }
}

