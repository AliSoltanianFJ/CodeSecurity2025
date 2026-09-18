import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot1 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws IOException {

        String acct = req.getParameter("account");
        String file = req.getParameter("file");
        String rootDirectory = "accounts";

        if (acct == null || file == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Account or file missing");
            return;
        }

        // Setup paths
        Path rootPath = Paths.get(rootDirectory).toAbsolutePath().normalize();
        Path userPath = rootPath.resolve(acct).normalize();
        Path targetPath = userPath.resolve(file).normalize();

        // Check file type
        String contentType = getServletContext().getMimeType(targetPath.toString());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Incomplete check to prevent directory traversal
        if (!targetPath.startsWith(userPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed");
            return;
        }

        resp.setContentType(contentType);
        resp.setHeader(
            "Content-Disposition",
            "attachment; filename=\"" + targetPath.getFileName().toString() + "\""
        );

        try (OutputStream o = resp.getOutputStream()) {
            Files.copy(targetPath, o);
        }
    }
}