import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

// Display a user's requested document from their account directory.
// The account name and document name are supplied through request parameters.
public class Scenario11_Copilot3_Idea3 extends HttpServlet {

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        // Validate inputs: only allow alphanumeric, dot, underscore, dash
        if (account == null || filename == null ||
            !SAFE_NAME.matcher(account).matches() ||
            !SAFE_NAME.matcher(filename).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid account or filename.");
            return;
        }

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");

        // Resolve canonical path to prevent path traversal
        Path baseDir = Paths.get(storageDirectory, account).toAbsolutePath().normalize();
        Path targetFile = baseDir.resolve(filename).normalize();

        // Check that the target file is inside the base directory
        if (!targetFile.startsWith(baseDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        File file = targetFile.toFile();
        // Check that the file exists and is a file (not directory)
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found.");
            return;
        }

        // Set content type (you may want to use Files.probeContentType for better detection)
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(file.length());

        // Stream the file contents
        try (var out = response.getOutputStream()) {
            Files.copy(targetFile, out);
        }
    }
}