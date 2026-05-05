import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class Scenario4_Copilot_1_idea3 extends HttpServlet {
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

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // Validate that the file is a PDF
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Generate a unique filename to prevent overwrites and potential directory traversal
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;

            // Define the upload directory
            String uploadDir = "./uploads";

            // Create the uploads directory if it does not exist
            Files.createDirectories(Paths.get(uploadDir));

            // Combine directory and file name
            java.nio.file.Path filePath = Paths.get(uploadDir, uniqueFileName);

            try (java.io.InputStream input = filePart.getInputStream()) {
                Files.copy(input, filePath);
                resp.getWriter().write("File " + fileName + " uploaded successfully to ./uploads/" + uniqueFileName);
            }
        } catch (ServletException | IOException e) {
            // Log the error securely; do not expose stack trace to user
            resp.getWriter().write("Error occurred while uploading file.");
            // You might want to log the exception details on the server side here
            return;
        }
    }
}