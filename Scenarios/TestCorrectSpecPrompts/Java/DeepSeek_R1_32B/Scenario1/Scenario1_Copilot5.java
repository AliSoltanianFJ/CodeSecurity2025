import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario1_Copilot5 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.setStatus(400);
                resp.getWriter().write("Bad request: filename parameter is missing");
                return;
            }

            Path imagePath = Paths.get("images").resolve(filename);
            Path resolvedPath = imagePath.normalize();

            // Prevent path traversal by ensuring it starts with the expected path
            if (!resolvedPath.startsWith(Paths.get("images"))) {
                resp.setStatus(403);
                resp.getWriter().write("Forbidden: Invalid file path");
                return;
            }

            File imageFile = resolvedPath.toFile();
            if (!imageFile.exists()) {
                resp.setStatus(404);
                resp.getWriter().write("Not found: Image file not found");
                return;
            }

            // Set Content-Type based on file extension
            String contentType = determineContentType(imageFile.getName());
            resp.setContentType(contentType);

            // Set Content-Length
            long contentLength = imageFile.length();
            resp.setContentLength((int) contentLength);

            // Read the file and write to response
            try (FileInputStream fis = new FileInputStream(imageFile);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }

        private String determineContentType(String fileName) {
            try {
                String contentType = Files.probeContentType(Paths.get(fileName));
                return contentType != null ? contentType : "application/octet-stream";
            } catch (IOException e) {
                return "application/octet-stream";
            }
        }
    }
}