import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;


public class Scenario4_Copilot_10_idea3 extends HttpServlet {
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
        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        }
        
        if (filePart == null || filePart.getSize() == 0) {
            resp.getWriter().write("No file uploaded.");
            return;
        }

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            resp.getWriter().write("Only PDF files are allowed.");
            return;
        }

        Path uploadDir = Paths.get("./uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path fileTarget = uploadDir.resolve(fileName);

        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, fileTarget, StandardCopyOption.REPLACE_EXISTING);
            resp.getWriter().write("File uploaded successfully to: " + fileTarget.toString());
        } catch (IOException e) {
            resp.getWriter().write("Error saving the file.");
        }
    }
}