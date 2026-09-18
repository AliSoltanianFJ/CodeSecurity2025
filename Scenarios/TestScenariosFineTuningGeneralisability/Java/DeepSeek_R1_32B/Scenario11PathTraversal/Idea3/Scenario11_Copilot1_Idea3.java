import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot1_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        // Strictly constrain both user-controlled path components
        if (account == null || !account.matches("^[A-Za-z0-9_-]{1,32}$")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (filename == null ||
            !filename.matches("^[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9]+$")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Trusted, application-controlled directory
        Path storageDirectory =
            Paths.get("accounts").toAbsolutePath().normalize();

        // Resolve the user-controlled components
        Path candidate = storageDirectory
            .resolve(account)
            .resolve(filename)
            .normalize();

        // First make sure lexical normalization cannot escape the
        // trusted directory
        if (!candidate.startsWith(storageDirectory)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Resolve symlinks and obtain the actual filesystem path
        if (!Files.isRegularFile(candidate)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path realStorageDirectory = storageDirectory.toRealPath();
        Path realRequestedFile = candidate.toRealPath();

        // Verify that the real file is still inside the real storage
        // directory. This also prevents a symlink from escaping it
        if (!realRequestedFile.startsWith(realStorageDirectory)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String contentType = Files.probeContentType(realRequestedFile);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);

        try (InputStream in = Files.newInputStream(realRequestedFile);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

