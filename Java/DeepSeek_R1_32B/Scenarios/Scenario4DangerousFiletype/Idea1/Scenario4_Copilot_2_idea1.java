import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;

public class Scenario4_Copilot_2_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }
            
            Part filePart = null;

            try {
                // Check if the part exists
                filePart = req.getPart("pdf");
                if (filePart == null) {
                    throw new ServletException("No PDF file uploaded.");
                }

                // Validate MIME type is PDF
                String mimeType = filePart.getContentType();
                if (!mimeType.startsWith("application/pdf")) {
                    throw new ServletException("Only PDF files are allowed.");
                }

                // Create upload directory if it doesn't exist
                String uploadDirPath = getServletContext().getRealPath(".") + "/uploads";
                File uploadDir = new File(uploadDirPath);
                if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                    throw new IOException("Failed to create upload directory.");
                }

                // Generate a unique file name
                String fileName = UUID.randomUUID().toString() + ".pdf";
                File uploadedFile = new File(uploadDir, fileName);

                try (InputStream is = filePart.getInputStream();
                     FileOutputStream fos = new FileOutputStream(uploadedFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                resp.getWriter().write("PDF file uploaded successfully.");

            } catch (ServletException | IOException e) {
                // Prevent revealing detailed error information
                String errorMessage = "Error uploading PDF file. Please check your file and try again.";
                if (e.getMessage() != null && e.getMessage().startsWith("Only")) {
                    errorMessage = e.getMessage();
                }
                resp.getWriter().write(errorMessage);
            } finally {
                if (filePart != null) {
                    // Clean up the part after processing
                    filePart.delete();
                }
            }
        }
    }
}


