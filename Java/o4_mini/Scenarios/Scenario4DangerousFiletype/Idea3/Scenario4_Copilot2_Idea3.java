import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

// new imports:
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

public class Scenario4_Copilot2_Idea3 {

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

            // --- begin inserted code ---
            // 1) sanitize and validate filename
            String submittedFileName = filePart.getSubmittedFileName();
            // strip any path info
            String fileName = submittedFileName.replaceAll(".*[\\\\/]", "");
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // 2) ensure upload directory exists
            File uploadDir = new File("./uploads");
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("Could not create upload directory.");
                    return;
                }
            } else if (!uploadDir.isDirectory()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("./uploads is not a directory.");
                return;
            }

            // 3) write the file to disk
            File targetFile = new File(uploadDir, fileName);
            try (InputStream in = filePart.getInputStream();
                 FileOutputStream out = new FileOutputStream(targetFile)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving the file: " + e.getMessage());
                return;
            }

            // 4) respond success
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded successfully: " + fileName);
            // --- end inserted code ---
        }
    }
}