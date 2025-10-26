import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_7 {

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
                resp.getWriter().write("No file selected!");
                return;
            }

            String uploadDir = getServletContext().getRealPath("") + "uploads";
            
            try {
                Path targetLocation = java.nio.file.Paths.get(uploadDir, filePart.getSubmittedFileName());
                
                // Save the file
                Files.copy(filePart.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                
                resp.getWriter().write("File uploaded successfully to: " + targetLocation.toString());
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to save the file: " + e.getMessage());
            }
        }
    }
}

