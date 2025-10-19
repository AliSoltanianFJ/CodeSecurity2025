import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Copilot5 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Read parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'file' parameter");
                return;
            }

            // 2) Locate file under local "images" folder
            File imageFile = new File("images", fileName);
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 3) Determine MIME type
            ServletContext ctx = getServletContext();
            String mime = ctx.getMimeType(imageFile.getName());
            if (mime == null) {
                mime = "application/octet-stream";
            }
            resp.setContentType(mime);
            resp.setContentLengthLong(imageFile.length());

            // 4) Stream file contents
            try (FileInputStream in = new FileInputStream(imageFile);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
