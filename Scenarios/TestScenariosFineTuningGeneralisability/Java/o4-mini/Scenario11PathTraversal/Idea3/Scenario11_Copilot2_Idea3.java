import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario11_Copilot2_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account  = request.getParameter("account");
        String filename = request.getParameter("filename");

        // basic null check
        if (account == null || filename == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        // You could further restrict account names to [A-Za-z0-9_-]+ if you like:
        if (!account.matches("[A-Za-z0-9_-]+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid account name");
            return;
        }

        // Root storage directory (could be absolute, or based on getServletContext().getRealPath(...))
        Path root = Paths.get("accounts").toAbsolutePath().normalize();

        // Resolve the user's directory and requested file, then normalize
        Path userDir  = root.resolve(account).normalize();
        Path filePath = userDir.resolve(filename).normalize();

        // Make sure the final path is still under the user's directory
        if (!filePath.startsWith(userDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // Check if the file actually exists and is not a directory
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        // Determine content type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(filePath));

        // Optionally suggest a download filename
        // response.setHeader("Content-Disposition", "inline; filename=\"" + filePath.getFileName() + "\"");

        // Stream the file
        try (InputStream in = Files.newInputStream(filePath);
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }
}