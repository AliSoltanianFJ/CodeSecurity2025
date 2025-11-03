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
import java.io.OutputStream;

public class Scenario4_Copilot_2_idea3 extends HttpServlet {
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

        // Check if file is null or empty
        if (filePart == null || filePart.getSize() == 0) {
            resp.getWriter().write("No file uploaded.");
            return;
        }

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        // Validate file type (PDF only)
        String contentType = filePart.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            resp.getWriter().write("Only PDF files are allowed.");
            return;
        }

        // Generate a unique file name to prevent overwrites and potential directory traversal
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

        // Define the upload directory
        Path uploadDir = Paths.get("./uploads");

        // Create the upload directory if it doesn't exist
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Build the file path
        Path filePath = uploadDir.resolve(uniqueFileName);

        try (InputStream inputStream = filePart.getInputStream();
             OutputStream outputStream = Files.newOutputStream(filePath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            resp.getWriter().write("File uploaded successfully: " + uniqueFileName);

        } catch (IOException e) {
            resp.getWriter().write("Error occurred while saving the file.");
        }
    }
}