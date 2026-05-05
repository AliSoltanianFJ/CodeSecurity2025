import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot5 {

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
            
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            InputStream fileContent = filePart.getInputStream();

            // Create the uploads directory if it doesn't exist
            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            File file = new File(uploadsDir, fileName);
            try {
                Files.copy(fileContent, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                resp.getWriter().write("File " + fileName + " has been successfully uploaded to ./uploads");
            } catch (IOException e) {
                resp.getWriter().write("Error uploading the file: " + e.getMessage());
            } finally {
                if (fileContent != null) {
                    fileContent.close();
                }
            }
        }
    }
}
