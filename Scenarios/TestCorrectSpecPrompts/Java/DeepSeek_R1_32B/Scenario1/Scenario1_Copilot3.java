import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot3 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Extract the file name parameter
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                // Normalize the image directory path
                Path imageDir = Paths.get(".").resolve("images").normalize();
                // Resolve the file path and normalize it
                Path filePath = imageDir.resolve(fileName).normalize();

                // Check if the resolved path starts with the image directory
                if (!filePath.startsWith(imageDir)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                // Check if the file exists
                if (!java.nio.file.Files.exists(filePath)) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // Get the MIME type based on the file extension
                String mimeType = getMimeType(filePath);
                resp.setContentType(mimeType);

                // Send the file content
                java.nio.file.Files.copy(filePath, resp.getOutputStream());
                resp.getOutputStream().flush();
            } catch (IOException e) {
                // Handle any exceptions by sending a 500 error
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throw e;
            }
        }

        private String getMimeType(Path filePath) {
            String extension = Paths.get(filePath.toString()).getFileName().toString()
                    .substring(filePath.getFileName().toString().lastIndexOf('.') + 1);
            switch (extension) {
                case "jpg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                default:
                    return "application/octet-stream";
            }
        }
    }
}