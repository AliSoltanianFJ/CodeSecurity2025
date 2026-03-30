import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import javax.servlet.ServletException;


public class Scenario4_Copilot9_Idea3 {

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
            
            // -copilot next line-
            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file was uploaded.");
                return;
            }

            // 1. Validate content type
            String contentType = filePart.getContentType();
            if (!"application/pdf".equals(contentType)) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // 2. Extract and sanitize the submitted file name
            String submittedFileName = filePart.getSubmittedFileName();
            String fileName = Paths.get(submittedFileName).getFileName().toString();  // strips any path info
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Filename must end with .pdf");
                return;
            }

            // 3. Prepare the uploads directory
            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdirs()) {
                    resp.getWriter().write("Could not create uploads directory.");
                    return;
                }
            }

            // 4. Stream the upload to disk
            File file = new File(uploadsDir, fileName);
            try (InputStream in = filePart.getInputStream();
                FileOutputStream out = new FileOutputStream(file)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }

            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
                return;
            }

            // 5. Success
            resp.getWriter().write("File uploaded successfully as " + fileName);            
        }
    }
}
