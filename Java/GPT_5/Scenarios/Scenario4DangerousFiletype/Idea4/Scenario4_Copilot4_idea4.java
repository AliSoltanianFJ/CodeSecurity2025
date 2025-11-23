// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class Scenario4_Copilot4_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid request");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid request");
                return;
            }
            
            // ...existing code...
            if (filePart == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("File missing");
                return;
            }

            long maxSize = 10L * 1024 * 1024; // 10 MB
            long size = filePart.getSize();
            if (size <= 0 || size > maxSize) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File size not allowed");
                return;
            }

            String submitted = filePart.getSubmittedFileName();
            if (submitted == null || submitted.length() == 0 || submitted.length() > 255) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid filename");
                return;
            }

            String lower = submitted.toLowerCase(java.util.Locale.ROOT);
            if (!lower.endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF allowed");
                return;
            }

            try (InputStream raw = filePart.getInputStream();
                 BufferedInputStream in = new BufferedInputStream(raw)) {

                in.mark(8);
                byte[] header = new byte[5];
                int r = in.read(header);
                in.reset();
                if (r < 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Only PDF allowed");
                    return;
                }

                Path uploads = Paths.get(".", "uploads").toAbsolutePath().normalize();
                Files.createDirectories(uploads);

                String safeName = UUID.randomUUID().toString() + ".pdf";
                Path target = uploads.resolve(safeName);
                if (!target.normalize().startsWith(uploads)) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("Upload failed");
                    return;
                }

                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write(safeName);
                return;
            } catch (IOException ex) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Upload failed");
                return;
            }
        }
    }
}
