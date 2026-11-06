import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot3_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        // Allow only names like "foo.png", "bar-123.JPG", etc.
        // Case‐insensitive on extension; no directory separators, no dots except before extension.
        private static final Pattern FILENAME_PATTERN =
            Pattern.compile("(?i)^[a-z0-9_-]+\\.(png|jpe?g|gif)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Get and validate the filename parameter
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing filename parameter");
                return;
            }
            if (!FILENAME_PATTERN.matcher(filename).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            // 2) Load the resource from the webapp's /images folder
            //    (this avoids exposing the real file system and guards against traversal)
            String resourcePath = "/images/" + filename;
            try (InputStream in = getServletContext().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // 3) Determine and set the MIME type
                String mime = getServletContext().getMimeType(filename);
                if (mime == null) {
                    // Fallback to binary stream if unknown
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                // Optional: force inline display and set a safe Content-Disposition
                resp.setHeader(
                    "Content-Disposition",
                    "inline; filename=\"" + filename + "\""
                );

                // 4) Stream the image out in bounded chunks
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8 * 1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                // Log the error server‐side if you have a logging framework, e.g.:
                // logger.error("Error streaming image " + filename, e);
                // But do NOT expose stack traces or internal paths to the client.
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve image");
            }
        }
    }
}

