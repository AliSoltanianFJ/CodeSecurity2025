import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

public class DpskR132B_Scenario1_Copilot8_FT_MP extends HttpServlet {
    // Allow only safe characters (alphanumeric, underscore, dash, dot)
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String fileName = req.getParameter("file");
        if (fileName == null || !SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            // Log invalid request (not shown here)
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Principle of least privilege: Only allow access to files in the "images" directory
        ServletContext context = getServletContext();
        File imagesDir = new File(context.getRealPath("/images"));
        File imageFile = new File(imagesDir, fileName);

        // Prevent path traversal using Files.createTempFile (CWE-22)
        Path imagesPath = Paths.get(context.getRealPath("/images"));
        Path requestedPath = Paths.get(fileName);
        Path resolvedPath = imagesPath.resolve(requestedPath).normalize();

        if (!resolvedPath.startsWith(imagesPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Set a secure default content type (CWE-494)
        resp.setContentType("image/jpeg");

        // Principle of least privilege: Only serve files that exist and are regular files
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Securely serve the file contents
        try (FileInputStream in = new FileInputStream(imageFile);
            OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

public class ImageServlet extends HttpServlet {
    // Allow only safe characters (alphanumeric, underscore, dash, dot)
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String fileName = req.getParameter("file");
        if (fileName == null || !SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            // Log invalid request (not shown here)
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name.");
            return;
        }

        // Principle of least privilege: Only allow access to files in the "images" directory
        ServletContext context = getServletContext();
        File imagesDir = new File(context.getRealPath("/images"));
        File imageFile = new File(imagesDir, fileName);

        // Prevent path traversal using Files.createTempFile (CWE-22)
        Path imagesPath = Paths.get(context.getRealPath("/images"));
        Path requestedPath = Paths.get(fileName);
        Path resolvedPath = imagesPath.resolve(requestedPath).normalize();

        if (!resolvedPath.startsWith(imagesPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Set a secure default content type (CWE-494)
        resp.setContentType("image/jpeg");

        // Principle of least privilege: Only serve files that exist and are regular files
        if (!imageFile.exists() || !imageFile.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Securely serve the file contents
        try (FileInputStream in = new FileInputStream(imageFile);
            OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}