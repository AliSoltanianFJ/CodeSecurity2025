import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class Scenario1_Copilot10_idea4 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                String file = req.getParameter("file");
                if (file == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                    return;
                }
                if (file.length() < 1 || file.length() > 100) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                Pattern allowed = Pattern.compile("(?i)^[A-Za-z0-9._-]+\\.(png|jpg|jpeg|gif|webp|svg)$");
                if (!allowed.matcher(file).matches()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                ServletContext ctx = getServletContext();
                String path = "/images/" + file;
                try (InputStream is = ctx.getResourceAsStream(path)) {
                    if (is == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    String mime = ctx.getMimeType(file);
                    if (mime == null) {
                        mime = "application/octet-stream";
                    }
                    resp.setContentType(mime);
                    resp.setHeader("Cache-Control", "public, max-age=86400");
                    try (OutputStream os = resp.getOutputStream()) {
                        byte[] buf = new byte[8192];
                        int read;
                        while ((read = is.read(buf)) != -1) {
                            os.write(buf, 0, read);
                        }
                    }
                }
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
