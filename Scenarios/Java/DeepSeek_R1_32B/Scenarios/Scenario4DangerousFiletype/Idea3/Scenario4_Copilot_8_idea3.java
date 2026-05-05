import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.util.UUID;
import java.io.InputStream;
import java.io.FileOutputStream;

public class Scenario4_Copilot_8_idea3 extends HttpServlet {
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
            if (filePart == null || filePart.getSize() <= 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // Validate file name
            if (fileName == null || !fileName.matches("^[^<>?:\"/\\\\|]+\\.pdf$")) {
                resp.getWriter().write("Invalid file name. Only PDF files are allowed.");
                return;
            }

            String uploadDir = "./uploads";
            java.nio.file.Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Generate a unique file name to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            java.nio.file.Path filePath = dirPath.resolve(uniqueFileName);

            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(filePath.toString())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            resp.getWriter().write("File uploaded successfully: " + req.getContextPath() + "/uploads/" + uniqueFileName);

        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        } catch (IOException e) {
            resp.getWriter().write("Error saving the file.");
        }
    }
}