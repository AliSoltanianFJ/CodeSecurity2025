import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.InvalidPathException;
import java.util.UUID;

public class Scenario4_Copilot6 {

    // ...existing code...

    @MultipartConfig
    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // validate part
            if (filePart == null) {
                resp.getWriter().write("No file uploaded with field name 'pdf'.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String submittedName = null;
            try {
                submittedName = filePart.getSubmittedFileName();
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                // Fall through � try header parsing if environment doesn't support getSubmittedFileName()
            }

            if (submittedName == null || submittedName.trim().isEmpty()) {
                // fallback: try to parse from content-disposition header
                String cd = filePart.getHeader("content-disposition");
                if (cd != null) {
                    for (String part : cd.split(";")) {
                        part = part.trim();
                        if (part.startsWith("filename=")) {
                            submittedName = part.substring(part.indexOf('=') + 1).trim().replace("\"", "");
                            break;
                        }
                    }
                }
            }

            if (submittedName == null || submittedName.trim().isEmpty()) {
                resp.getWriter().write("Uploaded file has no filename.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String lowerName = submittedName.toLowerCase();
            String contentType = filePart.getContentType() == null ? "" : filePart.getContentType().toLowerCase();

            // basic checks to allow only PDFs
            if (!lowerName.endsWith(".pdf") && !contentType.equals("application/pdf") && !contentType.startsWith("application/pdf")) {
                resp.getWriter().write("Only PDF files are accepted.");
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            // sanitize filename (keep only base name)
            String safeName = Paths.get(submittedName).getFileName().toString();
            // remove suspicious characters and limit length
            safeName = safeName.replaceAll("[\\\\/\\r\\n\\t]+", "_");
            if (safeName.length() > 200) safeName = safeName.substring(safeName.length() - 200);

            // add a unique prefix to avoid collisions
            String uniquePrefix = UUID.randomUUID().toString();
            String finalFileName = uniquePrefix + "_" + safeName;

            Path uploadsDir;
            try {
                uploadsDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            } catch (InvalidPathException ipe) {
                resp.getWriter().write("Server error: invalid upload directory path.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            try {
                Files.createDirectories(uploadsDir);
            } catch (IOException e) {
                resp.getWriter().write("Server error: cannot create upload directory.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            Path target = uploadsDir.resolve(finalFileName).normalize();
            // ensure target is inside uploadsDir
            if (!target.startsWith(uploadsDir)) {
                resp.getWriter().write("Server error: invalid target path.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            try (InputStream in = filePart.getInputStream()) {
                // copy stream to target (replace if exists)
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Server error: failed to save uploaded file.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("File uploaded successfully as: " + finalFileName);
        }
    }

    // ...existing code...
}