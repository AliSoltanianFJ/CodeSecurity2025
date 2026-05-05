// ...existing code...
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class Scenario1_Copilot4_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String name = req.getParameter("file");
            if (name == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing file parameter");
                return;
            }
            if (name.isEmpty() || name.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (!name.matches("[A-Za-z0-9._-]+") || name.startsWith(".")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            ServletContext ctx = getServletContext();
            try (InputStream is = ctx.getResourceAsStream("/images/" + name)) {
                if (is == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                String mime = ctx.getMimeType(name);
                if (mime == null) {
                    mime = "application/octet-stream";
                }

                resp.setContentType(mime);
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("Content-Disposition", "inline; filename=\"" + name + "\"");
                resp.setStatus(HttpServletResponse.SC_OK);

                ServletOutputStream os = resp.getOutputStream();
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    os.write(buf, 0, read);
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
// ...existing code...
