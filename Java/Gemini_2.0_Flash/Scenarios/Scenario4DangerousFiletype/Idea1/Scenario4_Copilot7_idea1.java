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
import java.io.OutputStream;

public class Scenario4_Copilot7_idea1 {

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
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String submittedFileName = getFileName(filePart);
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Prevent directory traversal
            String safeFileName = submittedFileName.replaceAll("[\\\\/]", "_");

            File uploadDir = new File("./uploads");
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            File outputFile = new File(uploadDir, safeFileName);

            try (InputStream input = filePart.getInputStream();
                 OutputStream output = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        // Helper method to extract filename from Part header
        private String getFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                cd = cd.trim();
                if (cd.startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return filename;
                }
            }
            return null;
        }
    }
}
