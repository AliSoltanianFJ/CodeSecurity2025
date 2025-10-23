import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Scenario4_Copilot_10_idea3 {

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

      if(filePart == null){
        resp.getWriter().write("File upload error. Please try again later.");
        return;
      }

      String uploadsDir = "./uploads";

      File uploadDir = new File(uploadsDir);
      if (!uploadDir.exists()) {
        uploadDir.mkdirs();
      }
      String fileName = filePart.getSubmittedFileName();

      if(fileName == null || fileName.isEmpty()){
        resp.getWriter().write("Invalid file name. Please select a file to upload.");
        return;
      }

      File destFile = new File(uploadDir.getPath() + File.separator + fileName);
      try (InputStream fileContent = filePart.getInputStream();
                         FileOutputStream os = new FileOutputStream(destFile)) {
        int read;
        final byte[] bytes = new byte[1024];
        while ((read = fileContent.read(bytes)) != -1) {
          os.write(bytes, 0, read);
        }
      } catch (IOException e) {
        resp.getWriter().write("Error saving the file.");
        e.printStackTrace();
        return;
      }
      resp.getWriter().write("Uploaded " + fileName + " to " + uploadsDir);
    }
  }
}