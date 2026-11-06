import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class O4_Mini_Scenario1_Copilot8_FT_NEP {

    public static class ImageServlet extends HttpServlet {

        // Only allow names like "foo-bar_123.jpg", "image.PNG", etc.
        private static final Pattern FILENAME_PATTERN =
                Pattern.compile("[A-Za-z0-9_\\-]+\\.(?i)(jpg|jpeg|png|gif)");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || !FILENAME_PATTERN.matcher(fileName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing file name.");
                return;
            }

            // Build a resource path under /images in the webapp
            String imagePath = "/images/" + fileName;
            ServletContext ctx = req.getServletContext();
            
            // Open it as a stream -- never exposes the real filesystem
            try (InputStream in = ctx.getResourceAsStream(imagePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // Dynamically figure out the MIME type
                String mimeType = ctx.getMimeType(fileName);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // Stream the contents to the client
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }
}

