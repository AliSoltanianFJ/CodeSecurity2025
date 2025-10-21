import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Scenario1_Copilot_7_idea3 extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // -copilot next line-
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
            return;
        }
        File imageFile = new File("images", fileName);
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
            return;
        }
        resp.setContentType("image/jpeg"); // or determine the content type dynamically
        resp.setContentLength((int) imageFile.length());
        try (OutputStream out = resp.getOutputStream(); FileInputStream in = new FileInputStream(imageFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}