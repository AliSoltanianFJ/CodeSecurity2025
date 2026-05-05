import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Copilot1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing");
                return;
            }

            Path imagePath = Paths.get("images", filename);

            if (!imagePath.normalize().startsWith(Paths.get("images").toAbsolutePath())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            try {
                byte[] imageData = Files.readAllBytes(imagePath);
                resp.setContentType("image/jpeg");
                resp.setContentLength(imageData.length);
                resp.getOutputStream().write(imageData);

            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
            }
        }
    }
}
