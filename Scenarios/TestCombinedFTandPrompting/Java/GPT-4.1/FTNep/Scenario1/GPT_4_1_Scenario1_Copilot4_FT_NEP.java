import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import java.util.Objects;
import java.nio.file.LinkOption;

public class GPT_4_1_Scenario1_Copilot4_FT_NEP extends HttpServlet {
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty() || !SAFE_FILE_NAME.matcher(fileName).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        ServletContext context = getServletContext();
        String imagesDir = context.getInitParameter("imagesDir");
        if(imagesDir == null){
            imagesDir= context.getRealPath("/images");
        }
        Path imagesPath;
        Path requestedFile;
        Path canonicalImagesPath;
        Path canonicalRequestedFile;
        try {
            imagesPath = Paths.get(imagesDir).toAbsolutePath().normalize();
            requestedFile = imagesPath.resolve(fileName).normalize();
            canonicalImagesPath = imagesPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
            canonicalRequestedFile = requestedFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (InvalidPathException | IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
            return;
        }

        if (!canonicalRequestedFile.startsWith(canonicalImagesPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        if (!Files.exists(canonicalRequestedFile) || !Files.isRegularFile(canonicalRequestedFile)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        String mimeType = context.getMimeType(canonicalRequestedFile.toString());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        resp.setContentType(mimeType);
        resp.setHeader("X-Content-Type-Options", "nosniff");

        long fileLength = Files.size(canonicalRequestedFile);
        if (fileLength > 10 * 1024 * 1024) {
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File too large.");
            return;
        }

        try (OutputStream os = resp.getOutputStream()) {
            Files.copy(canonicalRequestedFile, os);
        }
    }
}
