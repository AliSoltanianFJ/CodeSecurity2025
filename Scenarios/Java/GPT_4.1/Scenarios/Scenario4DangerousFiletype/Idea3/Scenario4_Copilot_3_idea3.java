import java.io.File;
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
import java.io.FileOutputStream;
import java.util.UUID;

public class Scenario4_Copilot_3_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        resp.setContentType("text/plain");

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.getWriter().write("Form must be of type multipart/form-data");
            return;
        }

        Part filePart = req.getPart("pdf");
        if (filePart == null || filePart.getSize() == 0) {
            resp.getWriter().write("No file uploaded or file is empty.");
            return;
        }

        // Validate file type
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            resp.getWriter().write("Only PDF files are allowed.");
            return;
        }

        // Generate a safe file name
        String safeFileName = UUID.randomUUID().toString() + ".pdf";

        // Create upload directory if it doesn't exist
        File uploadDir = new File("./uploads");
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            resp.getWriter().write("Failed to create upload directory.");
            return;
        }

        File file = new File(uploadDir, safeFileName);

        // Stream file content to disk
        try (InputStream fileContent = filePart.getInputStream();
            FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            resp.getWriter().write("File uploaded successfully as " + safeFileName);
        } catch (IOException ex) {
            resp.getWriter().write("Error saving the file: " + ex.getMessage());
            // Log the exception securely
        }
    }
}