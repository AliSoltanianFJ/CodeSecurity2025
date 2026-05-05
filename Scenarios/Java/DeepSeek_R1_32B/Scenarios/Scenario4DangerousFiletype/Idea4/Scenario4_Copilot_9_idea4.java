import javax.servlet.http.Part;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot_9_idea4 {

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
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String contentType = filePart.getContentType();

            // Validate PDF type and extension
            if (!"application/pdf".equals(contentType)) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid filename. Filename must end with .pdf");
                return;
            }

            // Check file size
            long fileSize = filePart.getSize();
            if (fileSize > MAX_FILE_SIZE) {
                resp.getWriter().write("File is too large. Maximum allowed size is 5MB.");
                return;
            }

            // Generate unique filename to prevent overwrites and path traversal
            String uniqueFileName = UUID.randomUUID() + ".pdf";
            Path uploadPath = Paths.get(UPLOAD_DIRECTORY, uniqueFileName);

            // Ensure the uploads directory exists
            if (!Files.exists(uploadPath.getParent())) {
                try {
                    Files.createDirectories(uploadPath.getParent());
                } catch (IOException e) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            try {
                filePart.write(uploadPath.toString());
                resp.getWriter().write("File uploaded successfully: " + uniqueFileName);
            } catch (Exception e) {
                resp.getWriter().write("Error saving the file. Please try again.");
                // Log the exception for debugging purposes
                System.err.println(e.getMessage());
            }
        }
    }
}


