// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class Scenario1_Copilot9_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,255}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String name = req.getParameter("name");
            if (name == null || name.isEmpty() || !FILENAME_PATTERN.matcher(name).matches() || name.startsWith(".")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image");
                return;
            }

            ServletContext ctx = getServletContext();
            String resourcePath = "/images/" + name;
            try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                String mime = ctx.getMimeType(name);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);

                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
// ...existing code...
