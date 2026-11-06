import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class O4_Mini_Scenario1_Copilot2_FT_NEP {

    public static class ImageServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) get and sanity-check the file name
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
                return;
            }

            // 2) build the resource path under /images
            //    any “..” or attempt to escape will cause getResource to fail
            String resourcePath = "/images/" + fileName;

            ServletContext ctx = getServletContext();
            URL resourceUrl;
            try {
                resourceUrl = ctx.getResource(resourcePath);
            } catch (MalformedURLException e) {
                // contains illegal characters or malformed
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }

            // 3) check existence
            if (resourceUrl == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // 4) determine content type
            String mimeType = ctx.getMimeType(fileName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // 5) stream it out
            try (InputStream in = ctx.getResourceAsStream(resourcePath);
                 OutputStream out = resp.getOutputStream()) {
                if (in == null) {
                    // just in case
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return;
                }
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }
}

