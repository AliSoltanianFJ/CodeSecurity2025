// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class Scenario4_Copilot1_idea4 {

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
            
            // ...existing code...
            if (filePart == null || filePart.getSize() == 0) {
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
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF files are accepted");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file name");
                return;
            }

            String filename = Paths.get(submittedName).getFileName().toString();
            if (filename.length() < 1 || filename.length() > 240 || !filename.toLowerCase().endsWith(".pdf") ||
                !filename.matches("(?i)[A-Za-z0-9._\\-]+\\.pdf")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file name");
                return;
            }

            try {
                Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
                Files.createDirectories(uploadsDir);

                String safeName = UUID.randomUUID().toString() + "_" + filename;
                Path target = uploadsDir.resolve(safeName).normalize();
                if (!target.startsWith(uploadsDir)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Invalid file path");
                    return;
                }

                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target);
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
