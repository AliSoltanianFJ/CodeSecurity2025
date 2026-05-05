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

public class Scenario4_Copilot_3_idea3 extends HttpServlet {
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        resp.setContentType("text/plain");

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.getWriter().write("Form must be of type multipart/form-data");
            return;
        }

        try {
            Part filePart = req.getPart("pdf");
            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file was uploaded.");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Generate a unique filename to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

            // Validate file size (e.g., maximum 5MB)
            if (filePart.getSize() > 5 * 1024 * 1024) {
                resp.getWriter().write("File is too large. Maximum allowed size is 5MB.");
                return;
            }

            // Resolve the upload directory safely
            Path uploadDir = Paths.get("./uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            Path file_path = uploadDir.resolve(uniqueFileName);

            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, file_path);
            }

            if (Files.exists(file_path) && filePart.getSize() == Files.size(file_path)) {
                resp.getWriter().write("File uploaded successfully: " + uniqueFileName);
            } else {
                // Clean up in case of failure
                if (Files.exists(file_path)) {
                    Files.delete(file_path);
                }
                resp.getWriter().write("Failed to save the file.");
            }

        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        }
    }
}