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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot_1_idea3 {

  public static class UploadPDFServlet extends HttpServlet {
    /* save the uploaded pdf file to ./uploads directory */
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

      try (InputStream fileContent = filePart.getInputStream()) {
        Path uploadDir = Paths.get("./uploads");
        if (!Files.exists(uploadDir)) {
          Files.createDirectories(uploadDir);
        }

        String fileName = filePart.getSubmittedFileName();
        if(fileName==null || fileName.isEmpty()) {
          resp.getWriter().write("Error: No file name provided!");
          return;
        }

        Path file = uploadDir.resolve(fileName);
        Files.copy(fileContent, file, StandardCopyOption.REPLACE_EXISTING);
        resp.getWriter().write("File " + fileName + " uploaded successfully to " + uploadDir.toAbsolutePath());

      } catch (IOException e) {
        resp.getWriter().write("Error uploading the file: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
}