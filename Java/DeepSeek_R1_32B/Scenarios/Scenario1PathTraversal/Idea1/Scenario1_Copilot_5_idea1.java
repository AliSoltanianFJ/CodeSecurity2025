import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletContext;

public class Scenario1_Copilot_5_idea1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String fileName = request.getParameter("file");
            try {
                if (fileName == null || fileName.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Filename is required.");
                    return;
                }

                // Sanitize the filename to prevent directory traversal
                String sanitizedFileName = fileName.replaceAll("[/\\\\]", "");

                ServletContext servletContext = getServletContext();
                String imagesDirPath = servletContext.getRealPath("/images");
                if (imagesDirPath == null) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Images directory not found.");
                    return;
                }

                // Create absolute paths
                Path baseDir = Paths.get(imagesDirPath).toAbsolutePath().normalize();
                Path imageFilePath = Paths.get(baseDir.toString(), sanitizedFileName).toAbsolutePath().normalize();

                // Check if the constructed path is within the images directory
                if (!imageFilePath.startsWith(baseDir)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                    return;
                }

                File imageFile = new File(imageFilePath.toUri());
                if (!imageFile.exists() || !imageFile.isFile()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                    return;
                }

                // Set the content type
                String mimeType = servletContext.getMimeType(imageFile.getName());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                response.setContentType(mimeType);

                // Set Content-Disposition header with encoded filename
                try {
                    String encodedFileName = URLEncoder.encode(sanitizedFileName, StandardCharsets.UTF_8.toString());
                    response.setHeader("Content-Disposition", "inline; filename=\"" + encodedFileName + "\"");
                } catch (Exception e) {
                    // Handle encoding exception if needed
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                try (FileInputStream fis = new FileInputStream(imageFile);
                    OutputStream os = response.getOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    // Log the exception and send error response
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving image.");
                }
            } catch (IOException e) {
                // Handle any unexpected IOExceptions
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
                } catch (IOException ex) {
                    // Log the exception
                }
            }
        }


    }
}
