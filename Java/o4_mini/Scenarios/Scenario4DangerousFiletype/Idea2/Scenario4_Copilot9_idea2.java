import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class Scenario4_Copilot9_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        // max upload 10 MB
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // must be multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            long size = filePart.getSize();
            if (size <= 0 || size > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file size. Must be 1–10 MB.");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("File name is missing.");
                return;
            }
            // strip any path elements, allow only simple filename
            String fileName = Paths.get(submittedName).getFileName().toString();
            if (!fileName.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Invalid content type.");
                return;
            }

            // prepare uploads directory
            Path uploadDir = Paths.get("uploads");
            try {
                if (Files.notExists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Could not create upload directory.");
                return;
            }

            // write file
            Path target = uploadDir.resolve(fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving the file.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
