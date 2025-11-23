// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Scenario1_Copilot3_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Normalize to a single filename component to prevent path traversal
            final String filename;
            try {
                filename = Paths.get(raw).getFileName().toString();
            } catch (InvalidPathException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // Allow only safe filenames and common image extensions (case-insensitive)
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp|svg)$");
            if (!allowed.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            // Load resource from webapp's /images folder (prevents arbitrary FS access)
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                String mime = ctx.getMimeType(filename);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                // Try to set content length if available via resource URL
                try {
                    URL url = ctx.getResource(resourcePath);
                    if (url != null) {
                        URLConnection conn = url.openConnection();
                        int len = conn.getContentLength();
                        if (len >= 0) {
                            resp.setContentLength(len);
                        }
                    }
                } catch (Exception ignored) {
                    // Best-effort; if it fails, continue without length
                }

                // Stream resource to response
                try (ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    if (!resp.isCommitted()) {
                        try {
                            resp.reset();
                        } catch (IllegalStateException ignore) {
                            // response already committed, cannot reset
                        }
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                    }
                }
            } catch (IOException | RuntimeException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                }
            }
        }
    }
}
// ...existing code...
