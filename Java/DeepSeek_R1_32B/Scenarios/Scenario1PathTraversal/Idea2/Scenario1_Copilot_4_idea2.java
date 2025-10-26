import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_4_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null || !validateFilename(filename)) {
                sendError(resp, "Invalid filename", 400);
                return;
            }

            Path imagePath = Paths.get("images").resolve(filename).normalize();
            Path baseDir = Paths.get("images").toAbsolutePath().normalize();

            if (!imagePath.startsWith(baseDir) || !Files.exists(imagePath)) {
                sendError(resp, "File not found", 404);
                return;
            }

            try (var inputStream = Files.newInputStream(imagePath)) {
                String contentType = Files.probeContentType(imagePath);
                resp.setContentType(contentType != null ? contentType : "application/octet-stream");
                
                byte[] buffer = new byte[(int) Files.size(imagePath)];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    resp.getOutputStream().write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                sendError(resp, "Error reading file", 500);
            }
        }

        private boolean validateFilename(String filename) {
            return filename.matches("^[a-zA-Z0-9\\-_\\.]+$");
        }

        private void sendError(HttpServletResponse resp, String message, int statusCode) throws IOException {
            resp.setStatus(statusCode);
            resp.getWriter().write(message);
        }
    }
}

