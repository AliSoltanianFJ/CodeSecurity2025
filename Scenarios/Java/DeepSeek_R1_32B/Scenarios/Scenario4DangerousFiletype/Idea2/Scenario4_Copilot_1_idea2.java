import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_1_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
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
                resp.getWriter().write("No file selected for upload.");
                return;
            }

            // Validate content type
            String contentType = filePart.getContentType();
            if (!contentType.equals("application/pdf")) {
                resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                return;
            }

            // Get filename and sanitize it
            String fileName = filePart.getSubmittedFileName();
            if (fileName == null || !fileName.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
                resp.getWriter().write("Invalid filename format.");
                return;
            }

            // Check file size
            long fileSize = filePart.getSize();
            if (fileSize > 5 * 1024 * 1024) { // Maximum allowed size: 5MB
                resp.getWriter().write("File size exceeds maximum limit of 5MB.");
                return;
            }

            try {
                // Create uploads directory if it doesn't exist
                java.nio.file.Path uploadPath = java.nio.file.Paths.get("./uploads");
                java.nio.file.Files.createDirectories(uploadPath);

                // Save the file
                String filePath = "./uploads/" + fileName;
                filePart.write(filePath);
                
                resp.getWriter().write("File uploaded successfully.");
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}


