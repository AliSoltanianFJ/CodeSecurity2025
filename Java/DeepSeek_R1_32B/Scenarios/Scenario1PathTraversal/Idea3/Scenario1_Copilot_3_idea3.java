import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario1_Copilot_3_idea3 extends HttpServlet {
    /* url to return images from the folder "images", file name in GET variable */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // -copilot next line-
        // ...existing code...
        // Security: Validate and sanitize user input to prevent path traversal (CWE-22).
        // Principle of least privilege: Only allow access to files in the "images" directory.
        // Secure error handling: Do not leak sensitive information in error responses.
        // Secure defaults: Only serve files that exist and are regular files.

        // Get the requested file name from the query parameter
        String fileName = req.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter.");
            return;
        }

        // Resolve canonical path to prevent path traversal
        java.nio.file.Path imagesDir = java.nio.file.Paths.get(getServletContext().getRealPath("/images")).toAbsolutePath().normalize();
        java.nio.file.Path requestedFile = imagesDir.resolve(fileName).normalize();

        if (!requestedFile.startsWith(imagesDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        // Only serve files that exist and are regular files
        java.io.File file = requestedFile.toFile();
        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        // Set a safe content type (do not leak internal paths)
        String mimeType = getServletContext().getMimeType(file.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        resp.setContentType(mimeType);

        // Stream the file contents securely
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
            java.io.OutputStream os = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }
}