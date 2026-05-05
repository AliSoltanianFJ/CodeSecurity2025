import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class Copilot9_idea1 {

    public static class ImageServlet extends HttpServlet {
        // Only allow filenames matching a strict pattern (e.g., letters, numbers, underscores, hyphens, and .jpg/.png extensions)
        private static final Pattern SAFE_FILENAME = Pattern.compile("^[\\w\\-]+\\.(jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Validate filename strictly
            if (!SAFE_FILENAME.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file name");
                return;
            }

            // Only serve files from /images/ via getResourceAsStream (never expose real path)
            String resourcePath = "/images/" + fileName;
            InputStream imageStream = getServletContext().getResourceAsStream(resourcePath);

            if (imageStream == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            // Set MIME type
            String mimeType = getServletContext().getMimeType(fileName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // Stream image data
            try (InputStream in = imageStream; OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file");
            }
        }
    }
}
