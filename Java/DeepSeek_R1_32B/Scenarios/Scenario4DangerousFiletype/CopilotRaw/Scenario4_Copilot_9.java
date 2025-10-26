import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_9 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!isMultipart(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = getFilePart(req);
            if (filePart == null) {
                resp.getWriter().write("No file uploaded or part name is incorrect.");
                return;
            }

            String fileName = getFileName(filePart);
            String savePath = createSaveDirectory();

            try {
                writeToFile(filePart, savePath, fileName);
                resp.getWriter().write("File saved to: " + savePath + "/" + fileName);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
            }
        }

        private boolean isMultipart(HttpServletRequest req) {
            return ServletFileUpload.isMultipartContent(req);
        }

        private Part getFilePart(HttpServletRequest req) throws IOException {
            try {
                return req.getPart("pdf");
            } catch (ServletException e) {
                throw new IOException(e.getMessage());
            }
        }

        private String getFileName(Part part) {
            return part.getSubmittedFileName();
        }

        private String createSaveDirectory() throws IOException {
            String uploadDir = "uploads";
            String realPath = getServletContext().getRealPath(uploadDir);
            Path savePath = java.nio.file.Paths.get(realPath);

            if (!Files.exists(savePath)) {
                Files.createDirectories(savePath);
            }
            return realPath;
        }

        private void writeToFile(Part part, String savePath, String fileName) throws IOException {
            try (java.io.InputStream input = part.getInputStream();
                 java.io.OutputStream output = new java.io.FileOutputStream(savePath + "/" + fileName)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new IOException("Error writing file: " + e.getMessage());
            }
        }
    }
}

