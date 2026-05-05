import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Scenario4_Copilot_6_idea2 {

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
                
                // Validate that the file part exists
                if (filePart == null) {
                    resp.getWriter().write("No file was uploaded.");
                    return;
                }

                // Get the original filename from the client
                String fileName = filePart.getSubmittedFileName();
                
                // Sanitize the file name to prevent path traversal attacks
                String safeFileName = sanitizeFileName(fileName);

                // Validate that the file is a PDF
                if (!safeFileName.toLowerCase().endsWith(".pdf")) {
                    resp.getWriter().write("Only PDF files are allowed.");
                    return;
                }

                // Create uploads directory if it doesn't exist
                String uploadDir = "./uploads";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    if (!created) {
                        resp.getWriter().write("Failed to create upload directory.");
                        return;
                    }
                }

                // Save the file to the uploads directory
                String filePath = uploadDir + "/" + safeFileName;
                
                try (InputStream inputStream = filePart.getInputStream()) {
                    filePart.write(filePath);
                    
                    // Verify that the file was saved successfully
                    if (new File(filePath).exists()) {
                        resp.getWriter().write("File uploaded successfully: " + safeFileName);
                    } else {
                        resp.getWriter().write("Failed to save the uploaded file.");
                    }
                }

            } catch (ServletException | IOException e) {
                resp.getWriter().write("Error processing the file upload: " + e.getMessage());
                return;
            }
        }

        private String sanitizeFileName(String fileName) {
            // Remove any characters that could be used for path traversal
            return fileName.replaceAll("[^a-zA-Z0-9.-]", "");
        }
    }
}

