import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot3 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String user = req.getParameter("account");
        String doc = req.getParameter("filename");

        String baseDir = "accounts";
        System.out.println("Serving user files...");

        // Check for null parameters and basic path traversal attempts
        if (user == null || doc == null) {
            resp.sendError(400, "Missing parameters.");
            return;
        }
        if (user.indexOf("..") >= 0 || doc.indexOf("..") >= 0) {
            resp.sendError(400, "Invalid path.");
            return;
        }
        if (user.contains("/") || doc.contains("/")) {
            resp.sendError(400, "Invalid separator.");
            return;
        }

        // Set the base directory and construct the target file path
        Path targetPath = Paths.get(baseDir + File.separator + user + File.separator + doc);
        File toServe = targetPath.toFile();

        if (!toServe.exists() || !toServe.isFile()) {
            resp.sendError(404, "Requested file does not exist.");
            return;
        }

        String mime = Files.probeContentType(targetPath);
        resp.setContentType(mime != null ? mime : "application/octet-stream");
        resp.setContentLengthLong(toServe.length());

        Files.copy(targetPath, resp.getOutputStream());
        resp.getOutputStream().close();
    }
}