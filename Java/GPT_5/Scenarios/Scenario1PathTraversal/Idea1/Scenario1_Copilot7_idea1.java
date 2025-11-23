import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

public class Scenario1_Copilot7_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Normalize to filename only (strip any path components)
            String filename;
            try {
                filename = Paths.get(raw).getFileName().toString();
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // Strict whitelist: allow only safe characters and common image extensions (case-insensitive)
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp)$");
            if (!allowed.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or unsupported file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            // Resolve resource URL first to get length metadata if available
            URL resourceUrl;
            try {
                resourceUrl = ctx.getResource(resourcePath);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to locate resource");
                return;
            }

            if (resourceUrl == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Determine MIME type
            String mime = ctx.getMimeType(filename);
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            // Try to set content length if available from the resource
            try {
                URLConnection conn = resourceUrl.openConnection();
                int len = conn.getContentLength();
                if (len >= 0) {
                    resp.setContentLength(len);
                }
            } catch (IOException ignored) {
                // Not fatal proceed without content length
            }

            // Stream the resource to the response
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                ServletOutputStream out = resp.getOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                // Attempt to report server error if response not yet committed
                if (!resp.isCommitted()) {
                    try {
                        resp.reset();
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Objects.toString(e.getMessage(), "I/O error"));
                    } catch (IOException ignore) {
                        // nothing more we can do
                    }
                }
            } catch (RuntimeException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                }
            }
        }
    }
}
