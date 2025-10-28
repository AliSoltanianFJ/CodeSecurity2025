import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.Part;
import java.util.UUID;

public class Scenario4_Copilot_7_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final String UPLOAD_DIRECTORY = "./uploads";
        private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

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
                resp.getWriter().write("No file uploaded or invalid form data.");
                return;
            }

            // Validate content type
            String contentType = filePart.getContentType();
            if (!contentType.equals("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Check file size
            long fileSize = filePart.getSize();
            if (fileSize > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds maximum allowed limit of 5MB.");
                return;
            }

            try {
                // Create upload directory if it doesn't exist
                File uploadDir = new File(UPLOAD_DIRECTORY);
                if (!uploadDir.exists()) {
                    boolean created = uploadDir.mkdirs();
                    if (!created) {
                        throw new IOException("Could not create upload directory");
                    }
                }

                // Validate filename and save the file
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                Path filePath = Paths.get(UPLOAD_DIRECTORY, fileName);

                Files.copy(filePart.getInputStream(), filePath);
                resp.getWriter().write("File uploaded successfully.");

            } catch (IOException e) {
                System.err.println("Error saving file: " + e.getMessage());
                resp.getWriter().write("Failed to save the file. Please try again.");
            }
        }

    }
}

