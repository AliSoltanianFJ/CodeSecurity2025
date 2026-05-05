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

public class Scenario1_Copilot5_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            final String filename;
            try {
                // strip any path components to prevent path traversal attempts
                filename = Paths.get(raw).getFileName().toString();
            } catch (InvalidPathException ipe) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // allow only safe filename characters and restrict to common image extensions
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp)$");
            if (!allowed.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or unsupported file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            // Try to obtain the resource stream from the webapp (prevents direct filesystem access)
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // Determine content type. Prefer servlet context, fall back to URL connection if available.
                String mime = ctx.getMimeType(filename);
                if (mime == null) {
                    try {
                        URL resUrl = ctx.getResource(resourcePath);
                        if (resUrl != null) {
                            URLConnection conn = resUrl.openConnection();
                            String ct = conn.getContentType();
                            if (ct != null && !ct.trim().isEmpty()) {
                                mime = ct;
                            }
                            long len = conn.getContentLengthLong();
                            if (len >= 0) {
                                resp.setContentLengthLong(len);
                            }
                        }
                    } catch (Exception ignored) {
                        // ignore and fallback below
                    }
                }

                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                // stream the content to the response output
                try (ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    // If streaming fails and response not committed, attempt to send error
                    if (!resp.isCommitted()) {
                        try {
                            resp.reset();
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                        } catch (IOException ignore) {
                            // nothing further can be done
                        }
                    }
                }
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error");
                }
            } catch (RuntimeException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                }
            }
        }
    }
}
