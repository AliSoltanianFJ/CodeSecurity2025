import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Copilot6 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing");
                return;
            }

            File file = new File("images", filename);
            if (!file.exists() || file.isDirectory()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            resp.setContentType("image/jpeg"); // Adjust content type as needed
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image");
            }
        }
    }
}
