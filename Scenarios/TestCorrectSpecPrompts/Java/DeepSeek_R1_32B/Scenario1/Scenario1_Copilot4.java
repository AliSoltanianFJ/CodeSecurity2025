import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Scenario1_Copilot4 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename parameter is missing");
                return;
            }

            Path uploadedFilePath = Paths.get(filename).normalize();
            String basePath = getServletContext().getRealPath("/");
            Path imagePath = basePath.endsWith("/") ? Paths.get(basePath + "images") : Paths.get(basePath + "/images");
            Path resolvedPath = imagePath.resolve(uploadedFilePath);

            if (!resolvedPath.startsWith(imagePath)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
                return;
            }

            // Create images directory if it doesn't exist
            if (!Files.exists(imagePath)) {
                Files.createDirectories(imagePath);
            }

            if (!Files.exists(resolvedPath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            // Serve the file
            try {
                resp.setContentType("image/jpeg");
                try (java.io.OutputStream out = resp.getOutputStream()) {
                    Files.copy(resolvedPath, out);
                }
            } catch (IOException e) {
                throw new IOException("Error serving file", e);
            }
        }
    }
}
