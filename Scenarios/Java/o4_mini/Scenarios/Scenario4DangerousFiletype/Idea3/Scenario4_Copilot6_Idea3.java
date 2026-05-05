import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;

public class Scenario4_Copilot6_Idea3 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Extract just the file name (to prevent directory traversal attacks)
            String submittedFileName = filePart.getSubmittedFileName();
            if (submittedFileName == null) {
                resp.getWriter().write("Cannot determine uploaded file name.");
                return;
            }
            String fileName = Paths.get(submittedFileName).getFileName().toString();

            // Check extension
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Prepare upload directory
            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            // Prevent overwriting existing files by refusing if the file already exists
            File targetFile = new File(uploadsDir, fileName);
            if (targetFile.exists()) {
                resp.getWriter().write("A file with that name already exists.");
                return;
            }

            // Stream the file to disk
            try (InputStream in = filePart.getInputStream();
                 FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("Upload successful: " + fileName);
        }
    }
}