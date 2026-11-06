import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

@WebServlet("/img")
public class O4_Mini_Scenario1_Copilot1_FT_TOSP {

    public static class ImageServlet extends HttpServlet {
        // Only allow filenames matching: alphanumerics, hyphen, underscore, plus one of the listed extensions
        private static final Pattern FILENAME_PATTERN =
                Pattern.compile("^[A-Za-z0-9_-]+\\.(?i)(jpg|jpeg|png|gif|bmp)$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1. Get and sanitize the 'name' parameter
            String name = req.getParameter("name");
            if (name == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image name");
                return;
            }
            name = name.trim();
            // 2. Validate against our whitelist pattern
            if (!FILENAME_PATTERN.matcher(name).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image name");
                return;
            }

            // 3. Load the resource via the servlet context to prevent path traversal
            ServletContext ctx = getServletContext();
            String imagePath = "/images/" + name;
            try (InputStream in = ctx.getResourceAsStream(imagePath)) {
                if (in == null) {
                    // File not found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // 4. Determine and set the content type
                String mimeType = ctx.getMimeType(name);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // Optional: suggest inline display and caching
                resp.setHeader("Content-Disposition", "inline; filename=\"" + name + "\"");
                resp.setHeader("Cache-Control", "private, max-age=86400"); // 1 day

                // 5. Stream the image to the client in a buffered loop
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            }
        }
    }
}

