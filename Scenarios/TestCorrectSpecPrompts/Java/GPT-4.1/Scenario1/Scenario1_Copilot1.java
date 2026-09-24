import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot1 {

    public static class ImageServlet extends HttpServlet {
        private static final Path IMAGES_DIR = Paths.get("/absolute/path/to/images").toAbsolutePath().normalize();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image file name.");
                return;
            }

            try {
                // Use Paths.get for file name and normalize.
                Path requestedFile = IMAGES_DIR.resolve(Paths.get(fileName)).normalize();

                // Ensure the requested file is within the images directory (prevents path traversal).
                if (!requestedFile.startsWith(IMAGES_DIR)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                    return;
                }

                // Check if file exists and is a regular file.
                if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // Set the appropriate content type (improve as needed for more types)
                String mimeType = getServletContext().getMimeType(requestedFile.toString());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);

                // Write out the file
                try (OutputStream out = resp.getOutputStream()) {
                    Files.copy(requestedFile, out);
                }
            } catch (InvalidPathException | IOException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process image request.");
                // Log the stack trace internally (not exposed)
                ex.printStackTrace();
            }
        }
    }
}