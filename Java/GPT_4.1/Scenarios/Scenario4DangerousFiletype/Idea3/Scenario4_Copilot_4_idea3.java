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
import java.io.PrintWriter;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Scenario4_Copilot_4_idea3 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario4_Copilot_4_idea3.class.getName());
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        if (!ServletFileUpload.isMultipartContent(req)) {
            out.println("Form must be of type multipart/form-data");
            return;
        }
        Part filePart = null;
        try {
            filePart = req.getPart("pdf");
        } catch (ServletException e) {
            resp.getWriter().println("Error retrieving the file part.");
            return;
        }
        // -copilot next line-
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        // Validate file type (only allow PDF)
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            out.println("Invalid file type. Only PDF files are allowed.");
            return;
        }

        // Validate file size (e.g., limit to 10MB)
        long fileSize = filePart.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            out.println("File size exceeds the limit of 10MB.");
            return;
        }

        // Sanitize file name to prevent path traversal
        fileName = UUID.randomUUID().toString() + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "");

        Path uploadDir = Paths.get("./uploads");
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create upload directory", e);
                out.println("Failed to create upload directory.");
                return;
            }
        }
        Path filePath = uploadDir.resolve(fileName);
        try (InputStream fileContent = filePart.getInputStream()) {
            Files.copy(fileContent, filePath, StandardCopyOption.REPLACE_EXISTING);
            out.println("File uploaded successfully to " + filePath.toString());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error uploading file", e);
            out.println("Error uploading file: " + e.getMessage());
        }
    }
}