import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

public class Scenario4_Copilot4 {

    public static class UploadPDFServlet extends HttpServlet {
        /* save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

            String submittedFileName = filePart.getSubmittedFileName();
            String fileName = Paths.get(submittedFileName).getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid file type. Only PDF is allowed.");
                return;
            }

            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdirs()) {
                    resp.getWriter().write("Failed to create uploads directory.");
                    return;
                }
            }

            File targetFile = new File(uploadsDir, fileName);
            try (InputStream in = filePart.getInputStream();
                 OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully: " + targetFile.getAbsolutePath());
        }
    }
}
