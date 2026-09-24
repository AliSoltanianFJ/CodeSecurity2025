import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot8 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String imageName = req.getParameter("file");
            if (imageName == null || imageName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
                return;
            }

            // The directory where images are stored, relative to ServletContext root
            String imagesDir = getServletContext().getRealPath("/images");
            Path imagesDirPath = Paths.get(imagesDir).toAbsolutePath().normalize();

            // The path for the requested file
            Path requestedImage = imagesDirPath.resolve(imageName).normalize();

            // Path traversal protection: ensure requestedImage starts with imagesDirPath
            if (!requestedImage.startsWith(imagesDirPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                return;
            }

            // Check if file exists and is a regular file
            if (!Files.exists(requestedImage) || !Files.isRegularFile(requestedImage)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
                return;
            }

            // Set a generic content type, or you can use ServletContext.getMimeType()
            String mimeType = getServletContext().getMimeType(requestedImage.toString());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            resp.setContentType(mimeType);
            resp.setContentLengthLong(Files.size(requestedImage));

            // Stream the file to the response
            try (var in = Files.newInputStream(requestedImage);
                 var out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                // Do NOT expose stack trace to the user
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file.");
            }
        }
    }
}