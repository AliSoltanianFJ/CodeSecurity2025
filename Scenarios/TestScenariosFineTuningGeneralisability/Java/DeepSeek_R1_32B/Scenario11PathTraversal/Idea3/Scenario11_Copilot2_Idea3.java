import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot2_Idea3 extends HttpServlet {

    // Secure configuration: Define allowed characters for account and filenames (alphanumeric, underscore, hyphen, period)
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+$");
    private static final String FILE_STORAGE_ROOT = "accounts";

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        // Validate input parameters (prevents path traversal, injection)
        if (account == null || filename == null ||
            account.isEmpty() || filename.isEmpty() ||
            !SAFE_NAME_PATTERN.matcher(account).matches() ||
            !SAFE_NAME_PATTERN.matcher(filename).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request parameters.");
            return;
        }

        // Resolve the file path safely
        Path requestedFile = Paths.get(FILE_STORAGE_ROOT, account, filename).normalize();
        Path rootDir = Paths.get(FILE_STORAGE_ROOT).toAbsolutePath().normalize();

        // Ensure the requested file is inside the storage directory (prevent traversal)
        if (!requestedFile.toAbsolutePath().startsWith(rootDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Check file existence and permissions
        File file = requestedFile.toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set content type based on MIME type (useful for browser rendering)
        String mimeType = Files.probeContentType(requestedFile);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        response.setContentType(mimeType);

        // Stream the file contents securely
        try (var input = Files.newInputStream(requestedFile);
            var output = response.getOutputStream()) {
            input.transferTo(output);
        }
    }
}