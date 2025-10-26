import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.*;

public class Scenario4_Copilot_5_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            try {
                Part filePart = req.getPart("pdf");
                if (filePart == null) {
                    resp.getWriter().write("No file selected for upload.");
                    return;
                }

                // Validate the filename and MIME type
                String fileName = Optional.ofNullable(filePart.getHeader("Content-Disposition"))
                        .map(h -> h.substring(h.indexOf("filename=\"") + 10))
                        .map(s -> s.replaceAll("[^a-zA-Z0-9._]", "_"))  // Sanitize filename
                        .orElse("unknown.pdf");

                if (!"application/pdf".equals(filePart.getContentType())) {
                    resp.getWriter().write("Invalid file type. Only PDF files are allowed.");
                    return;
                }

                // Define the upload directory and create it if necessary
                String uploadDir = getServletContext().getRealPath("./uploads");
                Path uploadsPath = Paths.get(uploadDir);
                if (!Files.exists(uploadsPath)) {
                    Files.createDirectories(uploadsPath);
                }

                // Save the file to disk
                try (InputStream inputStream = filePart.getInputStream();
                     OutputStream outputStream = Files.newOutputStream(uploadsPath.resolve(fileName))) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                // Delete the temporary file
                filePart.delete();

                resp.getWriter().write("File uploaded successfully: " + fileName);

            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part: " + e.getMessage());
            } catch (IOException e) {
                resp.getWriter().write("Error uploading the file: " + e.getMessage());
            }
        }
    }
}

