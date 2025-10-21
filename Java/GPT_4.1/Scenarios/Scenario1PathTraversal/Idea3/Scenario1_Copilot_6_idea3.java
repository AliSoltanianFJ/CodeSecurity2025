import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scenario1_Copilot_6_idea3 extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
            return;
        }

        Path imagesDir = Paths.get("images").toAbsolutePath().normalize();
        Path filePath = imagesDir.resolve(fileName).normalize();

        if (!filePath.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        resp.setContentType("image/jpeg"); // Consider using a library to detect MIME type
        try (var in = Files.newInputStream(filePath);
            var out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}