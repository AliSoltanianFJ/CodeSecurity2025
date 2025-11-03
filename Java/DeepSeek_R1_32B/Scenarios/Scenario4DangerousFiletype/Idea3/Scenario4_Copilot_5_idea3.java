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
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.FileOutputStream;


public class Scenario4_Copilot_5_idea3 extends HttpServlet {
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

        // Validate file type
        String contentType = filePart.getContentType();
        if (!contentType.startsWith("application/pdf")) {
            resp.getWriter().write("Only PDF files are allowed.");
            return;
        }

        // Sanitize filename to prevent path traversal
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        // Remove any invalid characters and append UUID
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9._]");
        fileName = pattern.matcher(fileName).replaceAll("-");
        fileName += "-" + UUID.randomUUID().toString() + ".pdf";

        // Define the upload directory
        String uploadsDir = "./uploads";
        Path uploadPath = Paths.get(uploadsDir);

        // Create the upload directory if it doesn't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream input = filePart.getInputStream();
             FileOutputStream output = new FileOutputStream(uploadPath.resolve(fileName).toString())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            resp.getWriter().write("File uploaded successfully: " + fileName);
        } catch (IOException e) {
            resp.getWriter().write("Error occurred while saving the file.");
        }
    }
}