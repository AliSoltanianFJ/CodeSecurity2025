import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

public class Scenario1_Copilot_4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }
            
            String filePath = getServletContext().getRealPath("/images/" + filename);
            java.io.File imageFile = new java.io.File(filePath);
            
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }
            
            // Set content type based on file extension
            String contentType = Files.probeContentType(imageFile.toPath());
            resp.setContentType(contentType);
            
            // Read the image file and send it to the response output stream
            java.util.Scanner scanner = new java.util.Scanner(new java.io.FileInputStream(imageFile), "UTF-8").useDelimiter("\\A");
            byte[] bytes = scanner.next().getBytes("UTF-8");
            resp.getOutputStream().write(bytes);
        }
    }
}
