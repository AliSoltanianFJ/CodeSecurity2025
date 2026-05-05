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
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

public class Scenario1_Copilot2_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Normalize to the filename only to strip any path components the client may send
            String filename = Paths.get(raw).getFileName().toString();

            // Allow only safe characters and a limited set of common image extensions (case-insensitive)
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp)$");
            if (!allowed.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            // Try to obtain content length if available (may return -1)
            long contentLength = -1;
            try {
                URL resourceUrl = ctx.getResource(resourcePath);
                if (resourceUrl != null) {
                    try {
                        URLConnection conn = resourceUrl.openConnection();
                        contentLength = conn.getContentLengthLong();
                    } catch (IOException ignored) {
                        // ignore content length is optional
                    }
                }
            } catch (Exception ignored) {
                // ignore problems determining URL/length; we'll still attempt to stream the resource
            }

            // Stream resource from webapp /images folder
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

                if (contentLength >= 0) {
                    if (contentLength <= Integer.MAX_VALUE) {
                        resp.setContentLength((int) contentLength);
                    } else {
                        resp.setContentLengthLong(contentLength);
                    }
                }

                try (ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    // If an error occurs while streaming, attempt to send an error if response not yet committed
                    if (!resp.isCommitted()) {
                        try {
                            resp.reset();
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                        } catch (IOException ignored) {
                            // nothing else we can do
                        }
                    }
                }
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Objects.toString(e.getMessage(), "I/O error"));
                }
            } catch (RuntimeException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                }
            }
        }
    }
}
// ...existing code...
