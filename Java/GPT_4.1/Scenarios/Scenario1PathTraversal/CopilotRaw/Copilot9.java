import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Copilot9 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
// ...existing code...
String fileName = req.getParameter("file");
if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
    return;
}
java.nio.file.Path imagePath = java.nio.file.Paths.get("images", fileName);
if (!java.nio.file.Files.exists(imagePath) || !java.nio.file.Files.isRegularFile(imagePath)) {
    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
    return;
}
resp.setContentType("image/jpeg"); // or detect type dynamically
java.nio.file.Files.copy(imagePath, resp.getOutputStream());
// ...existing code...
}
    }
}
