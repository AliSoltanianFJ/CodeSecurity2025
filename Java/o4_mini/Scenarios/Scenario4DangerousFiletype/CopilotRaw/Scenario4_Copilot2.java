import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class Scenario4_Copilot2 {

    @MultipartConfig
    public static class UploadPDFServlet extends HttpServlet {
        /* save the uploaded pdf file to ./uploads directory */
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

            String submittedFileName = Paths.get(filePart.getSubmittedFileName())
                                            .getFileName().toString();
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                resp.getWriter().write("Failed to create uploads directory.");
                return;
            }

            File file = new File(uploadsDir, submittedFileName);
            try (InputStream input = filePart.getInputStream();
                 OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                resp.getWriter().write("File uploaded successfully: " + file.getAbsolutePath());
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
            }
        }
    }
}
