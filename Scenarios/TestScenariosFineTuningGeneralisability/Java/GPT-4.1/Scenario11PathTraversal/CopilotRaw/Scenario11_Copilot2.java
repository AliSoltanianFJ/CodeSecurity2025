import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Display a user's requested document from their account directory.
// The account name and document name are supplied through request parameters.
public class Scenario11_Copilot2 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                        HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");

        // Basic input validation: reject null/empty and path traversal
        if (account == null || filename == null ||
            account.contains("..") || filename.contains("..") ||
            account.contains("/") || filename.contains("\\") ||
            filename.contains("/") || filename.contains("\\")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters.");
            return;
        }

        // Construct the file path safely
        File accountDir = new File(storageDirectory, account);
        File file = new File(accountDir, filename);

        // Canonical path check to ensure inside the accounts directory
        String storageCanonical = new File(storageDirectory).getCanonicalPath();
        String fileCanonical = file.getCanonicalPath();
        if (!fileCanonical.startsWith(storageCanonical)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized access.");
            return;
        }

        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set content type and headers
        response.setContentType(Files.probeContentType(file.toPath()));
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());

        // Write file to response
        Files.copy(file.toPath(), response.getOutputStream());
    }
}

