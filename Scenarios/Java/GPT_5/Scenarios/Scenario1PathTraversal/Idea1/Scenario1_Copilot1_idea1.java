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

public class Scenario1_Copilot1_idea1 {

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
                filename = Paths.get(raw).getFileName().toString();
            } catch (InvalidPathException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // Strict whitelist: filename + allowed image extensions (case-insensitive)
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
                // Unexpected issue locating resource
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
                // Disable caching from the URLConnection layer since servlet container handles caching headers
                conn.setUseCaches(false);
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to open resource");
                }
                return;
            }

            String mime = ctx.getMimeType(filename);
            if (mime == null) {
                mime = conn.getContentType();
            }
            if (mime == null) {
                mime = URLConnection.guessContentTypeFromName(filename);
            }
            if (mime == null) {
                mime = "application/octet-stream";
            }

            resp.setContentType(mime);
            long contentLength = conn.getContentLengthLong();
            if (contentLength >= 0) {
                resp.setContentLengthLong(contentLength);
            }

            try (InputStream in = conn.getInputStream();
                 ServletOutputStream out = resp.getOutputStream()) {

                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                // If an I/O error occurs while streaming, attempt to send a 500 if not committed
                if (!resp.isCommitted()) {
                    try {
                        resp.reset();
                    } catch (IllegalStateException ignore) {
                        // can't reset if response already committed
                    }
                    try {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                    } catch (IOException ignore) {
                        // give up if we cannot send error
                    }
                }
            }
        }
    }
}
