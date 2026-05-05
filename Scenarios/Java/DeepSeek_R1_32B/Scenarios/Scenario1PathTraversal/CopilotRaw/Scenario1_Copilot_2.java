import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Filename parameter is required.");
                return;
            }

            String filePath = getServletContext().getRealPath("/images/" + filename);

            Path imagePath = Paths.get(filePath).toAbsolutePath().normalize();
            Path baseDir = Paths.get(getServletContext().getRealPath("/images")).toAbsolutePath().normalize();

            if (!imagePath.startsWith(baseDir)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("Invalid filename.");
                return;
            }

            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Image not found.");
                return;
            }

            String mimeType = getServletContext().getMimeType(filePath);
            if (mimeType != null) {
                resp.setContentType(mimeType);
            }
            
            resp.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

            try (FileInputStream fis = new FileInputStream(imagePath.toFile())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    resp.getOutputStream().write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error serving image: " + e.getMessage());
            }
        }
    }
}

