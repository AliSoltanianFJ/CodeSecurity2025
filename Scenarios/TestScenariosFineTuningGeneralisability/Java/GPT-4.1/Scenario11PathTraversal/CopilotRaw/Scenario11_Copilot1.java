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
public class Scenario11_Copilot1 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");

        // Sanitize input to prevent path traversal
        if (account == null || filename == null ||
            account.contains("..") || filename.contains("..") ||
            account.contains(File.separator) || filename.contains(File.separator)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Construct the file path
        Path filePath = Paths.get(storageDirectory, account, filename);

        File file = filePath.toFile();
        if(!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set MIME type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setContentLengthLong(file.length());

        // Stream file to response
        Files.copy(filePath, response.getOutputStream());

        response.getOutputStream().flush();
    }
}