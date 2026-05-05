import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class Scenario4_Copilot5_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Must be multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be multipart/form-data");
                return;
            }

            // 2) Retrieve part
            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException | IOException e) {
                resp.getWriter().write("Error retrieving file part");
                return;
            }

            // 3) Validate part
            if (filePart == null || filePart.getSize() <= 0) {
                resp.getWriter().write("No file uploaded");
                return;
            }

            // 4) Sanitize and validate filename
            String submitted = filePart.getSubmittedFileName();
            if (submitted == null) {
                resp.getWriter().write("Invalid file name");
                return;
            }
            // strip any path info
            String fileName = Paths.get(submitted).getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed");
                return;
            }

            // 5) Validate content type
            String contentType = filePart.getContentType();
            if (!"application/pdf".equals(contentType)) {
                resp.getWriter().write("Invalid file content type");
                return;
            }

            // 6) Prepare upload directory
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            // 7) Build a safe, unique target path
            String safeName = UUID.randomUUID().toString() + "_" + fileName;
            Path target = uploadDir.resolve(safeName).normalize();
            if (!target.startsWith(uploadDir)) {
                resp.getWriter().write("Invalid file path");
                return;
            }

            // 8) Stream to disk
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // 9) Success
            resp.getWriter().write("File uploaded successfully as " + safeName);
        }
    }
}
