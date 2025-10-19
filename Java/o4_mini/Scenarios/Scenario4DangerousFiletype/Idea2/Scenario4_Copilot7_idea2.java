import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
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
import java.util.UUID;

public class Scenario4_Copilot7_idea2 {

    @WebServlet("/uploadPDF")
    @MultipartConfig(
        fileSizeThreshold   = 1024 * 1024,    // 1 MB
        maxFileSize         = 10 * 1024 * 1024, // 10 MB
        maxRequestSize      = 15 * 1024 * 1024  // 15 MB
    )
    public static class UploadPDFServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // must be multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Error: form must use multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (IllegalStateException | ServletException | IOException e) {
                resp.getWriter().write("Error: unable to retrieve file part");
                return;
            }

            // original filename, stripped of any path info
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || submittedName.trim().isEmpty()) {
                resp.getWriter().write("Error: invalid file name");
                return;
            }
            String fileName = Paths.get(submittedName).getFileName().toString();

            // only PDFs by extension
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Error: only .pdf files are allowed");
                return;
            }

            // only PDFs by MIME type
            if (!"application/pdf".equals(filePart.getContentType())) {
                resp.getWriter().write("Error: uploaded file is not a PDF");
                return;
            }

            // sanitize filename to safe chars
            String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");

            // generate a unique name
            String uniqueName = UUID.randomUUID().toString() + "-" + safeName;

            // ensure upload directory exists
            Path uploadDir = Paths.get("./uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            // resolve target and write
            Path target = uploadDir.resolve(uniqueName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target);
            } catch (IOException e) {
                resp.getWriter().write("Error: failed to save file");
                return;
            }

            resp.getWriter().write("Success: file saved as " + uniqueName);
        }
    }
}
