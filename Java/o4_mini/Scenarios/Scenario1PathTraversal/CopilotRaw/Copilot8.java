import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Copilot8 {

    public static class ImageServlet extends HttpServlet {
        /**
         * Serves a file from the /images folder. Expects a ?file=<filename> parameter.
         * Rejects any filename containing “..”, “/” or “\” to prevent directory traversal.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1) Get and validate filename parameter
            String fileName = req.getParameter("file");
            if (fileName == null
                || fileName.isEmpty()
                || fileName.contains("..")
                || fileName.contains("/")
                || fileName.contains("\\")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter.");
                return;
            }

            // 2) Resolve the real path of the images directory
            String imagesDirPath = req.getServletContext().getRealPath("/images");
            File imageFile = new File(imagesDirPath, fileName);

            // 3) Ensure the file exists and is a normal file
            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 4) Determine content type
            String mimeType = req.getServletContext().getMimeType(imageFile.getName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(imageFile.length());

            // 5) Stream the file to the response
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
