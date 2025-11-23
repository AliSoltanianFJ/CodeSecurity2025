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
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Scenario1_Copilot8_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Reduce to filename only to mitigate path traversal (e.g. "../../etc/passwd")
            final String filename;
            try {
                filename = Paths.get(raw).getFileName().toString();
            } catch (InvalidPathException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // Allow only safe characters and common image extensions (case-insensitive)
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp)$");
            if (!allowed.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or unsupported file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + filename;

            URL resourceUrl;
            try {
                resourceUrl = ctx.getResource(resourcePath);
            } catch (Exception e) {
                // Any problem resolving the resource -> server error
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to locate resource");
                }
                return;
            }

            if (resourceUrl == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            URLConnection conn;
            try {
                conn = resourceUrl.openConnection();
                conn.setUseCaches(false);
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to open resource");
                }
                return;
            }

            String mime = ctx.getMimeType(filename);
            if (mime == null) {
                // try URLConnection's content type as fallback
                mime = conn.getContentType();
            }
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            long contentLength = conn.getContentLengthLong();
            if (contentLength >= 0) {
                // set content length if known
                try {
                    resp.setContentLengthLong(contentLength);
                } catch (IllegalStateException ignored) {
                }
            }

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 ServletOutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                // If response not committed, try to report error; otherwise there is nothing we can do
                if (!resp.isCommitted()) {
                    try {
                        resp.reset();
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }
}
// ...existing code...
