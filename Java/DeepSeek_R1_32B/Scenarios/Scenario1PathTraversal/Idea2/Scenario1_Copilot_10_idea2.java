import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class Scenario1_Copilot_10_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // Sanitize the filename to prevent path traversal
            String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9_-]", "");
            if (sanitizedFilename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            ServletContext servletContext = getServletContext();
            File imageFile = new File(servletContext.getRealPath("/images/" + sanitizedFilename));
            
            // Check if the file exists and is not a directory
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // Set appropriate content type based on file extension
            String mimeType = servletContext.getMimeType(imageFile.getName());
            if (mimeType != null) {
                resp.setContentType(mimeType);
            } else {
                // Fallback to a default image type
                resp.setContentType("image/jpeg");
            }
            
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, length);
                }
            } catch (IOException e) {
                // Log the exception if needed
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}

