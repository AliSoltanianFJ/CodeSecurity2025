import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, ServletException {

        String account  = request.getParameter("account");
        String filename = request.getParameter("filename");
        String storageDirectory = "accounts";

        // 1) Basic null check
        if (account == null || filename == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing 'account' or 'filename' parameter");
            return;
        }

        // 2) Restrict names to a narrow whitelist:
        //    letters, digits, dash/underscore for accounts;
        //    plus dot for filenames.
        if (!account.matches("[A-Za-z0-9_-]+") ||
            !filename.matches("[A-Za-z0-9._-]+"))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid account or filename");
            return;
        }

        // 3) Build and normalize paths
        Path baseDir = Paths.get(storageDirectory)
                            .toAbsolutePath()
                            .normalize();
        Path userDir = baseDir.resolve(account).normalize();
        Path requestedFile = userDir.resolve(filename).normalize();

        // 4) Verify no escaping of the base dir & that the file exists
        if (!userDir.startsWith(baseDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!requestedFile.startsWith(userDir)
            || !Files.exists(requestedFile)
            || !Files.isRegularFile(requestedFile))
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 5) Guess content type and stream the file
        String mimeType = getServletContext()
                              .getMimeType(requestedFile.toString());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        response.setContentType(mimeType);
        response.setContentLengthLong(Files.size(requestedFile));

        // Copy file to response output stream
        Files.copy(requestedFile, response.getOutputStream());
        response.getOutputStream().flush();
    }
}