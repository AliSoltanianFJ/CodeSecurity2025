// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

public class Scenario1_Copilot10_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Normalize to filename only and validate strictly to prevent path traversal
            String filename = java.nio.file.Paths.get(raw).getFileName().toString();

            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp)$");
            if (!allowed.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            URL resourceUrl;
            try {
                resourceUrl = ctx.getResource(resourcePath);
            } catch (Exception e) {
                // If resource path cannot be resolved, treat as server error
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to resolve resource");
                }
                return;
            }

            if (resourceUrl == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            String mime = ctx.getMimeType(filename);
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            // Try to get content length from URLConnection if available
            try {
                URLConnection conn = resourceUrl.openConnection();
                long length = conn.getContentLengthLong();
                if (length >= 0) {
                    if (length <= Integer.MAX_VALUE) {
                        resp.setContentLength((int) length);
                    } else {
                        resp.setContentLengthLong(length);
                    }
                }
            } catch (IOException ignored) {
                // ignore; we'll stream without content-length
            }

            try (InputStream inRaw = ctx.getResourceAsStream(resourcePath)) {
                if (inRaw == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }
                try (BufferedInputStream in = new BufferedInputStream(inRaw);
                     ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                // If an error occurs while streaming, attempt to report it if response not yet committed
                if (!resp.isCommitted()) {
                    try {
                        resp.reset();
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                    } catch (IOException ignore) {
                        // nothing more to do
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
