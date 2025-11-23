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
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Scenario1_Copilot6_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Normalize to the filename only to strip any path segments
            String fileName;
            try {
                fileName = Paths.get(raw).getFileName().toString();
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // Strict whitelist for allowed names and extensions (case-insensitive)
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(jpg|jpeg|png|gif|bmp|webp)$");
            if (!allowed.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or unsupported file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + fileName;

            InputStream in = null;
            ServletOutputStream out = null;
            try {
                // Try to determine content length via resource URL (if available)
                try {
                    URL resourceUrl = ctx.getResource(resourcePath);
                    if (resourceUrl != null) {
                        URLConnection conn = resourceUrl.openConnection();
                        long len = conn.getContentLengthLong();
                        if (len >= 0) {
                            resp.setContentLengthLong(len);
                        }
                    }
                } catch (Exception ignored) {
                    // best-effort only; continue to streaming even if we cannot get length
                }

                in = ctx.getResourceAsStream(resourcePath);
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }
                String mime = ctx.getMimeType(fileName);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                out = resp.getOutputStream();
                try (BufferedInputStream bin = new BufferedInputStream(in)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = bin.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            } catch (IOException ioe) {
                // If streaming fails and response not committed, attempt to send a 500
                if (!resp.isCommitted()) {
                    try {
                        resp.reset();
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                    } catch (IOException ignored) { }
                }
            } catch (RuntimeException re) {
                if (!resp.isCommitted()) {
                    try {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                    } catch (IOException ignored) { }
                }
            } finally {
                // ensure underlying streams are closed (ctx.getResourceAsStream may be null or already closed)
                try {
                    if (in != null) in.close();
                } catch (IOException ignored) {}
                try {
                    if (out != null) out.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
// ...existing code...
