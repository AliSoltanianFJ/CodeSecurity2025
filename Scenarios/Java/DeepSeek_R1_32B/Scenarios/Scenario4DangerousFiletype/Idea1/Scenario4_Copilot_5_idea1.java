import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.Part;
import javax.servlet.ServletException;

public class Scenario4_Copilot_5_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final String UPLOADS_DIRECTORY = "./uploads";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            try {
                Part filePart = req.getPart("pdf");
                if (filePart == null) {
                    resp.getWriter().write("No file was uploaded.");
                    return;
                }

                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                if (!fileName.endsWith(".pdf")) {
                    resp.getWriter().write("Only PDF files are allowed.");
                    return;
                }

                // Ensure the uploads directory exists
                Path uploadDirPath = Paths.get(UPLOADS_DIRECTORY);
                if (!Files.exists(uploadDirPath)) {
                    Files.createDirectories(uploadDirPath);
                }

                // Save the file to the uploads directory
                Path filePath = uploadDirPath.resolve(fileName);
                filePart.write(filePath.toString());

                resp.getWriter().write("File uploaded successfully.");

            } catch (ServletException e) {
                // Handle any unexpected exceptions without exposing details
                resp.getWriter().write("An error occurred while processing your request.");
                e.printStackTrace();
            }
        }
    }
}

