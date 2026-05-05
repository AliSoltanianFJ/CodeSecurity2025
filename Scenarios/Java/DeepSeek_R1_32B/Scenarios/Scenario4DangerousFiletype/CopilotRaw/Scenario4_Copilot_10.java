import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Paths;

public class Scenario4_Copilot_10 {

    public static class UploadPDFServlet extends HttpServlet {
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

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Get the filename from the Part header
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            try {
                // Read the file content from the Part
                InputStream fileContent = filePart.getInputStream();
                byte[] bytes = new byte[fileContent.available()];
                fileContent.read(bytes);

                // Create uploads directory if it doesn't exist
                String uploadDir = getServletContext().getRealPath("uploads");
                Path path = Paths.get(uploadDir);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }

                // Save the file to uploads directory
                java.nio.file.Files.write(Paths.get(uploadDir, fileName), bytes);

                resp.getWriter().write("File saved successfully at: " + uploadDir + "/" + fileName);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file: " + e.getMessage());
                return;
            }
        }
    }
}


