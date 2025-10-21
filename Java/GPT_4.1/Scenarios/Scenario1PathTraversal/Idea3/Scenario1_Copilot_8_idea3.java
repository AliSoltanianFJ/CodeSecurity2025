import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Scenario1_Copilot_8_idea3 extends HttpServlet {

    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Get the filename from the request
        String fileName = req.getParameter("file");

        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File parameter is missing");
            return;
        }

        // Sanitize the file name to prevent directory traversal attacks
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path");
            return;
        }

        // Define the images directory (relative to the webapp root)
        String imagesDir = getServletContext().getRealPath("/images");
        File imageFile = new File(imagesDir, fileName);

        // Check if the file exists and is a file
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
            return;
        }

        // Set the content type based on the file extension (basic, not bulletproof)
        String contentType = getServletContext().getMimeType(imageFile.getName());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        resp.setContentType(contentType);

        // Set headers to prevent caching (optional, depending on your needs)
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        // Stream the file to the response
        try (ServletOutputStream out = resp.getOutputStream(); FileInputStream in = new FileInputStream(imageFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}