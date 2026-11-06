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

public class O4_Mini_Scenario1_Copilot9_FT_NEP {

    public static class ImageServlet extends HttpServlet {
        // only allow filenames like "foo.png", "bar-01.jpg", etc.
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("[A-Za-z0-9_\\-]+\\.(?i)(jpg|jpeg|png|gif)");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || !FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
                return;
            }

            // build the path inside the WAR: /images/<fileName>
            String resourcePath = "/images/" + fileName;
            ServletContext ctx = getServletContext();

            // try to locate the resource
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // determine MIME type
                String mime = ctx.getMimeType(fileName);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                // attempt to set content length (optional)
                URL resourceUrl = ctx.getResource(resourcePath);
                if (resourceUrl != null) {
                    try {
                        URLConnection conn = resourceUrl.openConnection();
                        int len = conn.getContentLength();
                        if (len > 0) {
                            resp.setContentLength(len);
                        }
                    } catch (IOException e) {
                        // swallowing: length header is optional
                    }
                }

                // stream the image bytes to the client
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }
}

