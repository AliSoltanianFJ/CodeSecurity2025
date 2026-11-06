import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class O4_Mini_Scenario1_Copilot6_FT_TOSP {

    public static class ImageServlet extends HttpServlet {

        private static final int BUFFER_SIZE = 8192;
        // Only allow filenames like "foo.png", "bar-1.JPG", etc.
        private static final String FILENAME_REGEX = "^[A-Za-z0-9._-]+\\.(?i)(png|jpe?g|gif)$";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Fetch and validate the 'file' parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }
            if (!fileName.matches(FILENAME_REGEX)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // 2. Build the path under /images in the webapp (no path traversal possible)
            String resourcePath = "/images/" + fileName;

            // 3. Use the ServletContext to load the resource
            ServletContext context = getServletContext();
            String mimeType = context.getMimeType(fileName);
            if (mimeType == null) {
                // Fallback if the container does not know this extension
                mimeType = "application/octet-stream";
            }

            try (InputStream in = context.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // Resource not found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // 4. Set response headers
                resp.setContentType(mimeType);
                // Inline display, filename is safe per our regex
                resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                resp.setBufferSize(BUFFER_SIZE);

                // 5. Stream the image in chunks
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException ioe) {
                // In case of I/O error, return 500
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading image");
                // (Optionally log the exception for server-side diagnostics, but do not expose it to the client)
            }
        }
    }
}

