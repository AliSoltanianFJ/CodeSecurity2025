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
import java.nio.file.Files;

public class Scenario4_Copilot6_idea1 {

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

            String submittedFileName = getSubmittedFileName(filePart);
            if (submittedFileName == null || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Prevent directory traversal
            String safeFileName = submittedFileName.replaceAll("[\\\\/]", "_");

            File uploadDir = new File("./uploads");
            if (!uploadDir.exists()) {
                try {
                    Files.createDirectories(uploadDir.toPath());
                } catch (IOException e) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            File outFile = new File(uploadDir, safeFileName);

            try (InputStream fileContent = filePart.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileContent.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }

        // Utility method to get the submitted file name from the Part
        private String getSubmittedFileName(Part part) {
            String header = part.getHeader("content-disposition");
            if (header == null) return null;
            for (String cd : header.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return filename;
                }
            }
            return null;
        }
    }
}
