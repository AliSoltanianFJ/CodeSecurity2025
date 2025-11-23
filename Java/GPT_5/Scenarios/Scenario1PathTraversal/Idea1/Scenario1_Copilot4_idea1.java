import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.regex.Pattern;

public class Scenario1_Copilot4_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String raw = req.getParameter("file");
            if (raw == null || raw.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // Strict filename whitelist: letters, digits, dot, underscore, dash + common image extensions
            Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpe?g|gif|bmp|webp)$");
            String fileName = raw.trim();
            if (!allowed.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + fileName;

            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                String mime = ctx.getMimeType(fileName);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                try (ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (IOException ioe) {
                    // If response can't be written, try to signal an error if not already committed
                    log("Error writing response for " + resourcePath, ioe);
                    if (!resp.isCommitted()) {
                        try {
                            resp.reset();
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending file");
                        } catch (IOException ignore) {
                        }
                    }
                }
            } catch (RuntimeException rte) {
                // Defensive: ensure no stack traces leak to client
                log("Unexpected error serving " + resourcePath, rte);
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Objects.toString(rte.getMessage(), "Server error"));
                }
            }
        }
    }
}