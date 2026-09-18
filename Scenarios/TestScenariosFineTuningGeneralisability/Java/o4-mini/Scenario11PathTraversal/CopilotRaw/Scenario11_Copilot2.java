
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot2 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account  = request.getParameter("account");
        String filename = request.getParameter("filename");
        String storageDirectory = "accounts";

        // basic null check
        if (account == null || filename == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing account or filename");
            return;
        }

        // build the base and account directories
        Path baseDir    = Paths.get(storageDirectory).toAbsolutePath().normalize();
        Path accountDir = baseDir.resolve(account).normalize();

        // resolve the requested file and normalize
        Path requestedFile = accountDir.resolve(filename).normalize();

        // determine mime type (fallback to binary)
        String mime = getServletContext().getMimeType(requestedFile.toString());
        if (mime == null) {
            mime = "application/octet-stream";
        }

        // also check that the requested file is within the account directory
        if (!requestedFile.startsWith(accountDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        response.setContentType(mime);
        response.setHeader(
            "Content-Disposition",
            "inline; filename=\"" + requestedFile.getFileName().toString() + "\""
        );

        // stream the file
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(requestedFile, out);
        }
    }
}