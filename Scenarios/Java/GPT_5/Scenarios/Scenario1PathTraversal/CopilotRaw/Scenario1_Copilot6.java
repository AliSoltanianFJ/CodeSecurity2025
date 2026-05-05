// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Scenario1_Copilot6 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Basic sanitisation: disallow path traversal and separators
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // Restrict to common image extensions
            String lower = fileName.toLowerCase();
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp"))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + fileName;
            // Use servlet context resource stream to avoid exposing arbitrary filesystem paths
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                    return;
                }

                String mime = ctx.getMimeType(fileName);
                if (mime == null) {
                    // fallback mapping
                    if (lower.endsWith(".png")) mime = "image/png";
                    else if (lower.endsWith(".gif")) mime = "image/gif";
                    else if (lower.endsWith(".bmp")) mime = "image/bmp";
                    else if (lower.endsWith(".webp")) mime = "image/webp";
                    else mime = "image/jpeg";
                }
                resp.setContentType(mime);

                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                // Ensure caller gets an appropriate HTTP error instead of stack trace
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image");
            }
        }
    }
}
// ...existing code...
