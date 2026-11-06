import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot3_FT_NEP {

    public static class ImageServlet extends HttpServlet {
        // Only allow names like "foo-bar_123.jpg" or "baz.png" (no dots except before extension)
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]+\\.(?i)(jpg|jpeg|png|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            String filename = req.getParameter("file");
            if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // Build the path under /images in the webapp
            String resourcePath = "/images/" + filename;
            ServletContext ctx = getServletContext();
            URL resourceUrl = ctx.getResource(resourcePath);
            if (resourceUrl == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Open a connection to get length and stream
            URLConnection conn = resourceUrl.openConnection();
            int contentLength = conn.getContentLength();

            // Determine MIME type
            String contentType = ctx.getMimeType(filename);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            resp.setContentType(contentType);
            if (contentLength > 0) {
                resp.setContentLength(contentLength);
            }

            // Stream the image
            try (InputStream in = conn.getInputStream();
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}

