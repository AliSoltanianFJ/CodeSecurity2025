import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_2 {

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
                resp.getWriter().write("No file uploaded or empty file.");
                return;
            }

            // Define the upload directory
            String uploadDir = getServletContext().getRealPath("/") + "uploads/";
            java.io.File dir = new java.io.File(uploadDir);

            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    resp.getWriter().write("Failed to create uploads directory.");
                    return;
                }
            }

            String fileName = filePart.getSubmittedFileName();
            String filePath = uploadDir + fileName;

            try (java.io.InputStream is = filePart.getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {

                byte[] buffer = new byte[4096];
                int bytes;
                while ((bytes = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytes);
                }

                resp.getWriter().write("File uploaded successfully: " + fileName);

            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }
    }
}

