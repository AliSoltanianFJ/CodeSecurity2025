import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.*;

public class Scenario4_Copilot_8 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
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
                resp.getWriter().write("No file uploaded or the uploaded file is empty.");
                return;
            }

            String uploadDir = getServletContext().getRealPath("uploads");
            File uploadsFolder = new File(uploadDir);

            // Ensure the uploads directory exists
            if (!uploadsFolder.exists()) {
                boolean created = uploadsFolder.mkdirs();
                if (!created) {
                    resp.getWriter().write("Error creating uploads directory.");
                    return;
                }
            }

            String fileName = filePart.getSubmittedFileName();
            Path uploadPath = Paths.get(uploadDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(uploadPath.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = filePart.getInputStream().read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                resp.getWriter().write("File uploaded successfully: " + fileName);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}

