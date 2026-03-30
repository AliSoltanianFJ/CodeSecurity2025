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

public class Scenario4_Copilot5_Idea3 {

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
            // ensure we actually got a file
            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // basic content‐type check
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                resp.getWriter().write("Uploaded file is not a PDF.");
                return;
            }

            // sanitize submitted file name to avoid path traversal
            String submittedName = filePart.getSubmittedFileName();
            String fileName = Paths.get(submittedName).getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("File does not have a .pdf extension.");
                return;
            }

            // prepare uploads directory
            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdirs()) {
                    resp.getWriter().write("Could not create upload directory.");
                    return;
                }
            }

            // target file on disk
            File uploadedFile = new File(uploadsDir, fileName);

            try (
                InputStream in = filePart.getInputStream();
                FileOutputStream out = new FileOutputStream(uploadedFile)
            ) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }

                resp.getWriter().write("Upload successful: " + uploadedFile.getAbsolutePath());
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
            }            
        }
    }
}
