import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class Scenario4_Copilot2_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final String UPLOAD_DIR = "uploads";
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File too large. Max size is " + MAX_FILE_SIZE + " bytes.");
                return;
            }

            String contentType = filePart.getContentType();
            if (!"application/pdf".equals(contentType)) {
                resp.getWriter().write("Invalid file type. Only PDFs allowed.");
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || !submittedName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Filename must end with .pdf");
                return;
            }

            // sanitize filename
            String baseName = Paths.get(submittedName).getFileName().toString();
            String safeName = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
            String finalName = UUID.randomUUID() + "_" + safeName;

            // resolve upload directory under webapp
            String appRoot = req.getServletContext().getRealPath("");
            Path uploadPath = Paths.get(appRoot, UPLOAD_DIR).toAbsolutePath().normalize();

            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                resp.getWriter().write("Server error: cannot create upload directory.");
                return;
            }

            Path targetFile = uploadPath.resolve(finalName).normalize();
            // ensure no path traversal
            if (!targetFile.getParent().equals(uploadPath)) {
                resp.getWriter().write("Invalid file path.");
                return;
            }

            try (var in = filePart.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error saving file.");
                return;
            }

            resp.getWriter().write("Upload successful: " + finalName);
        }
    }
}
