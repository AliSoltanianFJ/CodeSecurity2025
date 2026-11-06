import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;

public class O4_Mini_Scenario1_Copilot4_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Only allow filenames containing letters, digits, dot, underscore or hyphen.
        private static final Pattern FILENAME_PATTERN =
                Pattern.compile("^[A-Za-z0-9_.\\-]+$");

        // Whitelist of permitted image extensions:
        private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
                "jpg", "jpeg", "png", "gif", "bmp", "webp"
        );

        private static final Logger LOGGER =
                Logger.getLogger(ImageServlet.class.getName());

        @Override
        protected void doGet(HttpServletRequest req,
                             HttpServletResponse resp)
                throws IOException
        {
            // 1) Get & validate the "file" parameter
            String filename = req.getParameter("file");
            if (filename == null
                    || filename.isEmpty()
                    || !FILENAME_PATTERN.matcher(filename).matches())
            {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid file name.");
                return;
            }

            // 2) Enforce extension whitelist
            int dot = filename.lastIndexOf('.');
            if (dot < 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "File must have an extension.");
                return;
            }
            String ext = filename.substring(dot + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Unsupported file type.");
                return;
            }

            // 3) Build the path under /images in the webapp, preventing traversal
            String resourcePath = "/images/" + filename;
            ServletContext ctx = getServletContext();
            URL resourceUrl = ctx.getResource(resourcePath);
            if (resourceUrl == null) {
                // Not found
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 4) Open a URLConnection to read the resource
            URLConnection connection;
            try {
                connection = resourceUrl.openConnection();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,
                        "Cannot open resource: " + resourcePath, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 5) Determine mime type
            String mimeType = ctx.getMimeType(filename);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            // 6) Set headers
            resp.setContentType(mimeType);
            long length = connection.getContentLengthLong();
            if (length >= 0) {
                resp.setContentLengthLong(length);
            }
            // Example cache policy (adjust as needed)
            resp.setHeader("Cache-Control", "private, max-age=600");

            // 7) Stream the file in 8KB chunks
            try (InputStream in = connection.getInputStream();
                 ServletOutputStream out = resp.getOutputStream())
            {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Client aborted? Log and do not rethrow to avoid stack traces to client
                LOGGER.log(Level.WARNING,
                        "Error streaming image: " + filename, e);
            }
        }
    }
}

