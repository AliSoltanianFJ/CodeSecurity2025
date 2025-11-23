// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Scenario1_Copilot5_idea4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String name = req.getParameter("file");
            if (name == null || name.isEmpty() || name.length() > 255 || !name.matches("^[A-Za-z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
                return;
            }

            ServletContext ctx = getServletContext();
            String mime = ctx.getMimeType(name);
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);

            String imagesPath = ctx.getRealPath("/images");
            if (imagesPath != null) {
                try {
                    File base = new File(imagesPath).getCanonicalFile();
                    File target = new File(base, name).getCanonicalFile();
                    if (!target.getPath().startsWith(base.getPath() + File.separator) && !target.equals(base)) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    if (!target.isFile() || !target.canRead()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    long len = target.length();
                    if (len >= 0) {
                        resp.setContentLengthLong(len);
                    }
                    try (InputStream in = new FileInputStream(target);
                         OutputStream out = resp.getOutputStream()) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) != -1) {
                            out.write(buf, 0, r);
                        }
                    }
                    return;
                } catch (IOException | SecurityException ex) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            try (InputStream in = ctx.getResourceAsStream("/images/" + name)) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                try (OutputStream out = resp.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                }
            } catch (IOException | SecurityException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
// ...existing code...
