import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot6 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }

            // Images directory (relative to the servlet context)
            String imagesDir = getServletContext().getRealPath("/images");
            Path imagesDirPath = Paths.get(imagesDir).normalize();

            // Resolve the requested file name against the images directory
            Path requestedFile = imagesDirPath.resolve(fileName).normalize();

            // Check if the resolved file is *still* within the images directory to prevent traversal
            if (!requestedFile.startsWith(imagesDirPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Illegal file path.");
                return;
            }

            // Check if the file exists and is a regular file
            if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Set content type based on file
            String mimeType = getServletContext().getMimeType(requestedFile.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);

            // Write the file to the output stream
            try (OutputStream out = resp.getOutputStream()) {
                Files.copy(requestedFile, out);
            } catch (IOException e) {
                // Log internally, do not expose stack trace
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
            }
        }
    }
}