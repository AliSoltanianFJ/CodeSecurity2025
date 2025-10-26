import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario1_Copilot_3_idea2 {

    public static class ImageServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                return;
            }

            // Validate and sanitize the filename to prevent path traversal attacks
            String cleanedFilename = new File(filename).getName();
            if (!cleanedFilename.equals(StringEscapeUtils.escapeJava(cleanedFilename))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid characters in filename.");
                return;
            }

            // Get the real path of the images directory
            String imagesDir = getServletContext().getRealPath("/images");
            File imageFile = new File(imagesDir, cleanedFilename);

            if (!imageFile.exists() || !imageFile.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return;
            }

            // Prevent serving of arbitrary files outside the images directory
            if (!imageFile.getAbsolutePath().startsWith(new File(imagesDir).getCanonicalPath())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }

            // Set content type based on the file's extension
            String contentType = getServletContext().getMimeType(cleanedFilename);
            resp.setContentType(contentType);

            // Prevents MIME type sniffing and ensures proper handling of filenames with special characters
            resp.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + cleanedFilename);

            try (FileInputStream fis = new FileInputStream(imageFile);
                 java.io.OutputStream os = resp.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // Log the error
                getServletContext().log("Error serving image: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}

