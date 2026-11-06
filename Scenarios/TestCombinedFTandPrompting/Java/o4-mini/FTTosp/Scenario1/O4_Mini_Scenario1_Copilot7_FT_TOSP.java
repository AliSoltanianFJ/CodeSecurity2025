import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class O4_Mini_Scenario1_Copilot7_FT_TOSP {

    public static class ImageServlet extends HttpServlet {

        // Allowed image extensions
        private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp"};

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Retrieve and basic‐check the 'file' parameter
            String fileParam = req.getParameter("file");
            if (fileParam == null || fileParam.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }
            String fileName = fileParam.trim();

            // 2. Reject anything suspicious (path traversal, odd chars)
            //    Only allow letters, digits, dot, _, - 
            if (!fileName.matches("^[A-Za-z0-9._-]+$") || fileName.contains("..")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            // 3. Extension check
            String lower = fileName.toLowerCase();
            boolean okExt = false;
            for (String ext : ALLOWED_EXTENSIONS) {
                if (lower.endsWith(ext)) {
                    okExt = true;
                    break;
                }
            }
            if (!okExt) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            // 4. Build the resource path under /images in the webapp
            String resourcePath = "/images/" + fileName;
            ServletContext ctx = getServletContext();

            // 5. Attempt to load it as a resource
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }

                // 6. Set content type, disable MIME sniffing
                String mimeType = ctx.getMimeType(fileName);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);
                resp.setHeader("X-Content-Type-Options", "nosniff");

                // 7. Stream the image in safe, bounded chunks
                try (ServletOutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                // 8. Log internally, but do not expose stack trace or internals to client
                ctx.log("Error serving image: " + fileName, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to serve image");
            }
        }
    }
}

