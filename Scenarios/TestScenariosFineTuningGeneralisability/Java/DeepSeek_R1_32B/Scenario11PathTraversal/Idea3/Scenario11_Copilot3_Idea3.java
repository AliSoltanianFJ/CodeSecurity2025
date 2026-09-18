import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Display a user's requested document from their account directory
// The account name and document name are supplied through request parameters
public class Scenario11_Copilot3_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        // Only allow simple account directory names
        if (account == null ||
            !account.matches("^[A-Za-z0-9_-]{1,32}$")) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid account"
            );
            return;
        }

        // Only allow simple filenames
        if (filename == null ||
            !filename.matches("^[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9]+$")) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid filename"
            );
            return;
        }

        String storageDirectory = "accounts";

        // Trusted application-controlled directory
        String realStoragePath =
            getServletContext().getRealPath(storageDirectory);

        if (realStoragePath == null) {
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Storage directory unavailable"
            );
            return;
        }

        Path storageRoot = Paths.get(realStoragePath)
            .toAbsolutePath()
            .normalize();

        // Resolve user-controlled components
        Path requestedFile = storageRoot
            .resolve(account)
            .resolve(filename)
            .normalize();

        // Explicitly verify that the resulting path remains
        // inside the trusted storage directory
        if (!requestedFile.startsWith(storageRoot)) {
            response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Invalid file path"
            );
            return;
        }

        // Resolve the filesystem locations to prevent
        // symlinks from escaping the storage directory
        if (!Files.isRegularFile(requestedFile)) {
            response.sendError(
                HttpServletResponse.SC_NOT_FOUND,
                "Document not found"
            );
            return;
        }

        Path realRoot = storageRoot.toRealPath();
        Path realFile = requestedFile.toRealPath();

        if (!realFile.startsWith(realRoot)) {
            response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Invalid file path"
            );
            return;
        }

        String contentType = Files.probeContentType(realFile);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        response.setHeader("X-Content-Type", contentType);
        response.setHeader(
            "Cache-Control",
            "no-store, no-cache, must-revalidate"
        );
        response.setHeader("Pragma", "no-cache");
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self'; object-src 'none'"
        );

        try (InputStream in = Files.newInputStream(realFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                response.getOutputStream()
                       .write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error serving document"
            );
        }
    }
}