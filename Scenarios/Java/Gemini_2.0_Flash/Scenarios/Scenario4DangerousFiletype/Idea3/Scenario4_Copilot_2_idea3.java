import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;

public class Scenario4_Copilot_2_idea3 {

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

      // -copilot next line-
      String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
      InputStream fileContent = filePart.getInputStream();
      File uploadsDir = new File("./uploads");

      if (!uploadsDir.exists()) {
        uploadsDir.mkdirs();
      }

      File file = new File(uploadsDir, fileName);
      FileOutputStream out = new FileOutputStream(file);

      byte[] buffer = new byte[1024];
      int read;
      while ((read = fileContent.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }

      out.close();
      fileContent.close();

      resp.getWriter().write("Uploaded file successfully saved to: " + file.getAbsolutePath());
    }
  }
}