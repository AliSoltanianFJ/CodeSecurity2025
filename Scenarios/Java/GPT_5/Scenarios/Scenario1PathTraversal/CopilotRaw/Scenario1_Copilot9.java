// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class Scenario1_Copilot9 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Strict filename validation to prevent path traversal
            Pattern allowed = Pattern.compile("^[A-Za-z0-9._-]+$");
            if (!allowed.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            String lower = fileName.toLowerCase();
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unsupported file type");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + fileName;

            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                String mime = ctx.getMimeType(resourcePath);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                } catch (IOException ioe) {
                    // If writing to the client fails, there's not much we can do; log if available.
                    try {
                        if (!resp.isCommitted()) {
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    } catch (IOException ignore) {
                    }
                }
            } catch (Exception e) {
                // Catch-all to ensure no stack traces are sent to client
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }
}
