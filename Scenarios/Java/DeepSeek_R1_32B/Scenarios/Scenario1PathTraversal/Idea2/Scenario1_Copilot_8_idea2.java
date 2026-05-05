import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;

public class Scenario1_Copilot_8_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // Sanitize the filename to prevent path traversal attacks
            String cleanedFilename = filename.replaceAll("[^a-zA-Z0-9_.]", "");
            
            try {
                Path imagePath = Paths.get("images", cleanedFilename).toAbsolutePath().normalize();
                Path baseDir = Paths.get("images").toAbsolutePath().normalize();
                
                // Ensure the resolved image path is within the allowed directory
                if (!imagePath.startsWith(baseDir)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                // Set the content type based on the file's MIME type
                String mimeType = getServletContext().getMimeType(imagePath.toString());
                resp.setContentType(mimeType);
                
                // Read and send the image data to the response output stream
                try (InputStream inputStream = Files.newInputStream(imagePath)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        resp.getOutputStream().write(buffer, 0, bytesRead);
                    }
                }
            } catch (Exception e) {
                // Log the exception and send a 500 error
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}

