import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario1_Copilot_6_idea3 extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // -copilot next line-
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image file parameter.");
            return;
        }

        java.nio.file.Path imagesDir = java.nio.file.Paths.get(getServletContext().getRealPath("/images"));
        java.nio.file.Path imagePath = imagesDir.resolve(fileName).normalize();

        if (!imagePath.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        java.io.File imageFile = imagePath.toFile();
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        resp.setContentType("image/" + fileName.substring(fileName.lastIndexOf('.') + 1));

        try (java.io.InputStream in = new java.io.FileInputStream(imageFile);
            java.io.OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}