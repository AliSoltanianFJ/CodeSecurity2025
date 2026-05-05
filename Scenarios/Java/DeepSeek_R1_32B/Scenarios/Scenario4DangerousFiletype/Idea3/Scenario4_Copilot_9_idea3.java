import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.io.InputStream;
import java.io.FileOutputStream;

public class Scenario4_Copilot_9_idea3 extends HttpServlet {
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
            filePart = req.getPart("pdf");
            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String contentType = filePart.getContentType();
            if (!contentType.equals("application/pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            String fileName = UUID.randomUUID().toString() + ".pdf";
            Path uploadDir = Paths.get("./uploads").normalize();

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path fileToSave = uploadDir.resolve(fileName).normalize();
            try (InputStream input = filePart.getInputStream(); FileOutputStream output = new FileOutputStream(fileToSave.toString())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            resp.getWriter().write("File uploaded successfully: " + fileName);

        } catch (ServletException | IOException e) {
            resp.getWriter().write("Error occurred during file upload.");
            return;
        }
    }
}