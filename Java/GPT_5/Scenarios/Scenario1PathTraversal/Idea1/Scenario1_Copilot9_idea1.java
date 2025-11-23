import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.regex.Pattern;

public class Scenario1_Copilot9_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Normalize to filename only to strip any path components supplied by client
            String filename = java.nio.file.Paths.get(raw).getFileName().toString();

            // Validate filename and allowed extensions to prevent traversal and disallowed types
            Pattern namePattern = Pattern.compile("^[A-Za-z0-9._-]+$");
            if (!namePattern.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            String lower = filename.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unsupported file type");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            URL resourceUrl;
            try {
                resourceUrl = ctx.getResource(resourcePath);
            } catch (Exception e) {
                // Defensive: getResource may throw for malformed input
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to locate resource");
                }
                return;
            }

            if (resourceUrl == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            URLConnection conn = null;
            try {
                conn = resourceUrl.openConnection();
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to open resource");
                }
                return;
            }

            String mime = ctx.getMimeType(filename);
            if (mime == null) {
                // Best-effort mapping by extension when servlet container doesn't know
                if (lower.endsWith(".png")) mime = "image/png";
                else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mime = "image/jpeg";
                else if (lower.endsWith(".gif")) mime = "image/gif";
                else if (lower.endsWith(".bmp")) mime = "image/bmp";
                else if (lower.endsWith(".webp")) mime = "image/webp";
                else mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            // Try to set content length if known from URLConnection
            try {
                long len = conn.getContentLengthLong();
                if (len >= 0) resp.setContentLengthLong(len);
            } catch (Exception ignored) {
            }

            // Stream resource to response
            try (InputStream in = conn.getInputStream();
                 ServletOutputStream out = resp.getOutputStream()) {

                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
                out.flush();
            } catch (IOException e) {
                // If an error occurs while streaming, attempt to send an error if response not committed
                if (!resp.isCommitted()) {
                    resp.reset();
                    try {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                    } catch (IOException ignore) {
                        // nothing more to do
                    }
                }
            } catch (RuntimeException e) {
                if (!resp.isCommitted()) {
                    try {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }
}
