import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class Scenario1_Copilot7_idea4 {

    public static class ImageServlet extends HttpServlet {
        private static final Pattern FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,255}\\.(?i)(png|jpe?g|gif|webp|svg)$");
        private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String name = req.getParameter("file");
            if (name == null || name.isBlank() || name.length() > 255) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }
            if (!FILENAME_PATTERN.matcher(name).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
                return;
            }

            String ext = "";
            int idx = name.lastIndexOf('.');
            if (idx >= 0 && idx + 1 < name.length()) {
                ext = name.substring(idx + 1).toLowerCase(Locale.ROOT);
            }
            if (!ALLOWED_EXT.contains(ext)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type");
                return;
            }

            Path base;
            try {
                base = Paths.get(System.getProperty("user.dir"), "images").toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException ex) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource directory not available");
                return;
            }

            Path target = base.resolve(name).normalize();
            if (!target.startsWith(base) || !Files.isRegularFile(target)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String contentType = Files.probeContentType(target);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            try {
                long size = Files.size(target);
                if (size >= 0 && size <= Integer.MAX_VALUE) {
                    resp.setContentLength((int) size);
                } else if (size > Integer.MAX_VALUE) {
                    resp.setHeader("Content-Length", Long.toString(size));
                }
            } catch (IOException ignored) {
            }
            resp.setHeader("Cache-Control", "private, max-age=3600");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setStatus(HttpServletResponse.SC_OK);

            try (InputStream in = Files.newInputStream(target); OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int r;
                while ((r = in.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                }
                out.flush();
            } catch (IOException e) {
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to read file");
                }
            }
        }
    }
}
