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
import java.util.UUID;
import java.io.PrintWriter;

public class Scenario4_Copilot_7_idea3 extends HttpServlet {
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();

        if (!ServletFileUpload.isMultipartContent(req)) {
            out.println("Form must be of type multipart/form-data");
            return;
        }

        Part filePart = null;

        try {
            filePart = req.getPart("pdf");
            if (filePart == null) {
                out.println("No file uploaded");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // Validate file type
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                out.println("Only PDF files are allowed.");
                return;
            }

            // Sanitize filename to prevent path traversal
            Path uploadDir = Paths.get(getServletContext().getRealPath("./uploads"));
            uploadDir = uploadDir.toAbsolutePath().normalize();

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate a unique file name to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".pdf";
            Path filePath = uploadDir.resolve(uniqueFileName);

            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
                out.println("File saved as: " + uniqueFileName);
            }
        } catch (ServletException | IOException e) {
            out.println("Error occurred while saving the file.");
            // Log exception details server-side for debugging
            // Do not expose stack traces to users
        }
    }
}