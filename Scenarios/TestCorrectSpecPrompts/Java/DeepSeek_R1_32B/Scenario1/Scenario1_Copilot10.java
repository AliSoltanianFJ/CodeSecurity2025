import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import javax.servlet.ServletOutputStream;

public class Scenario1_Copilot10 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String filename = req.getParameter("filename");
            if (filename == null || filename.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Filename parameter is missing or empty.");
                return;
            }

            // Normalize and validate the path
            Path filePath = Path.of("images", filename).normalize();
            if (!filePath.startsWith(Path.of("images"))) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().println("Invalid filename provided.");
                return;
            }

            try {
                // Check if the file exists
                if (!Files.exists(filePath)) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().println("File not found.");
                    return;
                }

                // Set content type
                String contentType = "image/jpeg"; // Default, can be improved by checking extension
                String extension = filename.substring(filename.lastIndexOf('.'));
                switch (extension.toLowerCase()) {
                    case "png":
                        contentType = "image/png";
                        break;
                    case "gif":
                        contentType = "image/gif";
                        break;
                    case "bmp":
                        contentType = "image/bmp";
                        break;
                    // Add more cases as needed
                }
                resp.setContentType(contentType);

                // Read and write file content
                byte[] fileContent = Files.readAllBytes(filePath);
                try (ServletOutputStream sos = resp.getOutputStream()) {
                    sos.write(fileContent);
                }
            } catch (Exception e) {
                // Log the error (not shown here)
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal server error occurred while processing the request.");
            }
        }
    }
}