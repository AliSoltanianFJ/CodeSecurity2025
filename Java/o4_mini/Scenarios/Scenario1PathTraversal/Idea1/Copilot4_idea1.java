import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Copilot4_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Only allow filenames with safe characters (alphanumeric, dot, dash, underscore)
            if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file name");
                return;
            }

            // Prevent access to hidden files or files starting with dot
            if (fileName.startsWith(".")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                return;
            }

            // Only allow certain extensions (e.g., png, jpg, jpeg, gif)
            String lowerName = fileName.toLowerCase();
            if (!(lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                  lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unsupported file type");
                return;
            }

            // Load image from resources (WEB-INF/images)
            String resourcePath = "/WEB-INF/images/" + fileName;
            InputStream imageStream = getServletContext().getResourceAsStream(resourcePath);

            if (imageStream == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String mimeType = getServletContext().getMimeType(fileName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            try (InputStream in = imageStream; OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file");
            }
        }
    }
}
