import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_1_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            // Validate the filename to prevent directory traversal and other attacks
            try {
                Path baseDir = Paths.get(getServletContext().getRealPath("/images"));
                Path requestedFile = baseDir.resolve(filename).normalize();
                if (!requestedFile.startsWith(baseDir)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid file path.");
                    return;
                }

                if (!Files.exists(requestedFile)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // Set the content type based on the file
                String mimeType = getServletContext().getMimeType(filename);
                resp.setContentType(mimeType);

                // Serve the image
                Files.copy(requestedFile, resp.getOutputStream());
            } catch (IllegalArgumentException e) {
                // Handle cases where filename might be invalid or cause issues
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename.");
            }
        }
    }
}

