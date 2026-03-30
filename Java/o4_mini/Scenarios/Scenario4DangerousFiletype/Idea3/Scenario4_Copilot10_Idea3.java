import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot10_Idea3 {

    public static class UploadPDFServlet extends HttpServlet {
        /* Save the uploaded PDF file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Validate multipart request
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Error: form must be multipart/form-data");
                return;
            }

            // 2) Retrieve the file part
            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error: unable to retrieve file part");
                return;
            }
            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("Error: no file uploaded");
                return;
            }

            // 3) Check content type
            String contentType = filePart.getContentType();
            if (contentType == null || !"application/pdf".equalsIgnoreCase(contentType)) {
                resp.getWriter().write("Error: only PDF files are allowed");
                return;
            }

            // 4) Sanitize and extract filename
            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null) {
                resp.getWriter().write("Error: file name not found");
                return;
            }
            // strip any path information
            String fileName = Paths.get(submittedName).getFileName().toString();
            // allow only [A-Za-z0-9._-] in name
            fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Error: file must have .pdf extension");
                return;
            }

            // 5) Determine upload directory (relative to webapp root)
            String uploadRoot = getServletContext().getRealPath("");
            File uploadDir = new File(uploadRoot, "uploads");
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                resp.getWriter().write("Error: unable to create upload directory");
                return;
            }

            // 6) Write the file
            File target = new File(uploadDir, fileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.getWriter().write("Error: failed to save file");
                return;
            }

            // 7) Success
            resp.getWriter().write("Upload successful: " + fileName);
        }
    }
}