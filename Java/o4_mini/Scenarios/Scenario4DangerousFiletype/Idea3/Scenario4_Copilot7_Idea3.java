import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;

public class Scenario4_Copilot7_Idea3 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
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
            if (filePart == null) {
                resp.getWriter().write("No file uploaded under ‘pdf’ field.");
                return;
            }

            // 1. Get submitted filename and normalize it
            String submittedFileName = filePart.getSubmittedFileName();
            if (submittedFileName == null) {
                resp.getWriter().write("Cannot determine uploaded file name.");
                return;
            }
            // Strip any path info
            String fileName = Paths.get(submittedFileName).getFileName().toString();

            // 2. Enforce .pdf extension
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // 3. Ensure upload directory exists
            File uploadDir = new File("./uploads");
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            // 4. Stream the file out
            File outFile = new File(uploadDir, fileName);
            try (InputStream in = filePart.getInputStream();
                OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
                return;
            }

            resp.getWriter().write("File uploaded successfully as " + fileName);
        }
    }
}
