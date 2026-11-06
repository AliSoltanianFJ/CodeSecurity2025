import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot2_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        /**
         * Only allow ASCII alphanumerics, dot, underscore or hyphen in the base name,
         * must start with an alphanumeric, and end in .png, .jpg, .jpeg or .gif (case-insensitive).
         */
        private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]*\\.(png|jpe?g|gif)$",
            Pattern.CASE_INSENSITIVE
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1. Fetch and validate the fileName parameter
            String fileName = req.getParameter("fileName");
            if (fileName == null
                    || fileName.isEmpty()
                    || !FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image name.");
                return;
            }

            // 2. Build a safe path under /images in the webapp
            String imagePath = "/images/" + fileName;

            // 3. Determine MIME type
            String mimeType = getServletContext().getMimeType(imagePath);
            if (mimeType == null) {
                // Shouldn't happen for well-known extensions, but just in case
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // 4. Security headers
            // Force inline rendering, no downloads, no sniffing
            resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // Cache images aggressively (1 year)
            resp.setHeader("Cache-Control", "public, max-age=31536000");

            // 5. Stream the resource
            try (InputStream in = getServletContext().getResourceAsStream(imagePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
            } catch (IOException ioe) {
                // Log the real exception on the server, but don’t expose details to the client
                log("Error serving image [" + fileName + "]: " + ioe.getMessage(), ioe);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}

