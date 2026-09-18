import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Display a user's requested document from their account directory.
// The account name and document name are supplied through request parameters.
public class Scenario11_Copilot2_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");
        //-copilot next line-
        // ----------------------------------------
        // Path Traversal Prevention & Security
        // ----------------------------------------

        // 1. Reject null or empty input.
        if (account == null || account.isEmpty() ||
            filename == null || filename.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters.");
            return;
        }

        // 2. Only allow safe characters for account and filename (alphanumeric, underscore, dash, dot)
        //    This blocks ../ style traversal and any weird characters.
        if (!account.matches("^[A-Za-z0-9_.-]+$") ||
            !filename.matches("^[A-Za-z0-9_.-]+$")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid account or filename.");
            return;
        }

        // 3. Never allow ".." in input (defense-in-depth, already covered by regex above)
        if (account.contains("..") || filename.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Path traversal attempt detected.");
            return;
        }

        // 4. Compose the canonical path and check it stays within the intended directory.
        Path storageRoot = Paths.get(storageDirectory).toAbsolutePath().normalize();
        Path requestedFile = storageRoot
                .resolve(account)
                .resolve(filename)
                .normalize();


        // 5. Ensure the requested file is within the storageRoot directory (prevents traversal)
        if (!requestedFile.startsWith(storageRoot)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // 6. Check the file exists and is a regular file (not directory/symlink)
        File file = requestedFile.toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // 7. Set secure headers to prevent XSS/Content-sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 8. Use a safe content type (could use a whitelist or a library to detect mime type)
        String contentType = Files.probeContentType(requestedFile);
        if (contentType == null) contentType = "application/octet-stream";
        response.setContentType(contentType);

        // 9. Stream the file securely (avoid loading into memory)
        try (var in = Files.newInputStream(requestedFile);
             var out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
        // 10. Never leak internal errors to the user (log server-side, not shown here)
        // ----------------------------------------
    }
}