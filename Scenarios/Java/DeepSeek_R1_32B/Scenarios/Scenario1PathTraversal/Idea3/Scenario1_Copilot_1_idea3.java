import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Scenario1_Copilot_1_idea3 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Get the requested file name from the "file" parameter
        String fileName = req.getParameter("file");
        // Basic validation to prevent path traversal
        if (fileName == null
                || fileName.isEmpty()
                || fileName.contains("..")
                || fileName.contains("/") 
                || fileName.contains("\\")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Resolve the real path to the "images" folder
        String imagesDir = getServletContext().getRealPath("/images");
        File imageFile = new File(imagesDir, fileName);

        // Ensure the file is inside the images directory
        File imagesCanonical = new File(imagesDir).getCanonicalFile();
        File requestedCanonical = imageFile.getCanonicalFile();

        if (!requestedCanonical.getPath().startsWith(imagesCanonical.getPath())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Check that the file exists and is a regular file
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Set the content type based on the file's extension
        String mimeType = getServletContext().getMimeType(imageFile.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        resp.setContentType(mimeType);
        resp.setContentLengthLong(imageFile.length());

        // Stream the file contents to the response
        try (FileInputStream in = new FileInputStream(imageFile);
            java.io.OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}