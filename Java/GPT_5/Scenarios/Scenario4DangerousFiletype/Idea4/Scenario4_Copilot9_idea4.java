// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.util.UUID;

public class Scenario4_Copilot9_idea4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

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
            
            if (filePart == null || filePart.getSize() <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file provided");
                return;
            }

            final long MAX_SIZE = 10L * 1024L * 1024L; // 10 MB
            if (filePart.getSize() > MAX_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File too large");
                return;
            }

            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF allowed");
                return;
            }

            String submitted = filePart.getSubmittedFileName();
            if (submitted == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file");
                return;
            }
            String baseName = submitted;
            int idx = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
            if (idx >= 0) baseName = baseName.substring(idx + 1);
            int dot = baseName.lastIndexOf('.');
            if (dot < 0 || !baseName.substring(dot + 1).equalsIgnoreCase("pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF allowed");
                return;
            }

            try {
                Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
                Files.createDirectories(uploadDir);

                String safeName = UUID.randomUUID().toString() + ".pdf";
                Path target = uploadDir.resolve(safeName);

                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }

                try (InputStream in = Files.newInputStream(target)) {
                    byte[] header = new byte[5];
                    int read = in.read(header);
                    String sig = read == 5 ? new String(header, "ISO-8859-1") : "";
                    if (!sig.startsWith("%PDF")) {
                        Files.deleteIfExists(target);
                        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                        resp.getWriter().write("Invalid PDF file");
                        return;
                    }
                }

                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("File uploaded");
            } catch (IOException ex) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server error");
            }
        }
    }
}
