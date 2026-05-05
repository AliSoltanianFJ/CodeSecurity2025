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
import java.io.InputStream;

public class Scenario4_Copilot_2_idea3 extends HttpServlet {
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
            resp.getWriter().write("No file uploaded or file is empty.");
            return;
        }

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            resp.getWriter().write("Only PDF files are allowed.");
            return;
        }

        Path uploadPath = Paths.get("./uploads");
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }
        }

        Path filePath = uploadPath.resolve(fileName);

        try (InputStream fileContent = filePart.getInputStream()) {
            Files.copy(fileContent, filePath);
            resp.getWriter().write("File uploaded successfully to " + filePath.toString());
        } catch (IOException e) {
            resp.getWriter().write("File upload failed: " + e.getMessage());
        }
    }
}
