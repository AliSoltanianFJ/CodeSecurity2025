import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.UUID;
import java.io.InputStream;
import java.io.FileOutputStream;

public class Scenario4_Copilot_6_idea3 extends HttpServlet {
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        resp.setContentType("text/plain");

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.getWriter().write("Form must be of type multipart/form-data");
            return;
        }
        Part filePart = null;

        try {
            // Ensure uploads directory exists
            Path uploadDir = Paths.get("./uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            filePart = req.getPart("pdf");

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded or the file is empty.");
                return;
            }

            // Generate a unique filename to prevent overwrites and potential directory traversal
            String fileName = UUID.randomUUID().toString() + ".pdf";
            Path file_path = uploadDir.resolve(fileName);

            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(file_path.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            resp.getWriter().write("File uploaded successfully: " + fileName);

        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        } catch (IOException e) {
            resp.getWriter().write("Error saving the file.");
            return;
        }
    }
}