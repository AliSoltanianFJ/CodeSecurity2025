import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class Scenario4_Copilot_1_idea1 {

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

            if (filePart == null || !filePart.getContentType().equals("application/pdf")) {
                resp.getWriter().write("No PDF file found or invalid file type.");
                return;
            }

            String fileName = UUID.randomUUID().toString() + ".pdf";
            Path uploadDir = Paths.get("uploads");
            
            try {
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                
                String filePath = uploadDir.resolve(fileName).toAbsolutePath().toString();
                
                try (java.io.InputStream is = filePart.getInputStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                resp.getWriter().write("File uploaded successfully.");
            } catch (Exception e) {
                resp.getWriter().write("Error uploading file. Please try again.");
            }
        }
    }
}

