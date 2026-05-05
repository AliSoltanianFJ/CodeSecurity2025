import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Scenario4_Copilot4 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }
            
            // ...existing code...
            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String submittedFileName = null;
            try {
                submittedFileName = filePart.getSubmittedFileName();
            } catch (NoSuchMethodError ignored) {
                // older servlet API may not have getSubmittedFileName; fallback to header parsing
                String contentDisp = filePart.getHeader("content-disposition");
                if (contentDisp != null) {
                    for (String cdPart : contentDisp.split(";")) {
                        cdPart = cdPart.trim();
                        if (cdPart.startsWith("filename=")) {
                            submittedFileName = cdPart.substring(cdPart.indexOf('=') + 1).trim().replace("\"", "");
                            break;
                        }
                    }
                }
            }

            if (submittedFileName == null || submittedFileName.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Uploaded file has no filename.");
                return;
            }

            // sanitize filename to avoid path traversal
            String safeFileName = Paths.get(submittedFileName).getFileName().toString();

            // basic validation: check extension and/or content type
            String contentType = filePart.getContentType();
            boolean looksLikePdf = false;
            if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                looksLikePdf = true;
            } else if (safeFileName.toLowerCase().endsWith(".pdf")) {
                looksLikePdf = true;
            }

            if (!looksLikePdf) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Uploaded file is not a PDF.");
                return;
            }

            Path uploadDir = Paths.get(".", "uploads");
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Unable to create upload directory.");
                return;
            }

            Path target = uploadDir.resolve(safeFileName);
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to save uploaded file: " + e.getMessage());
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded to: " + target.toAbsolutePath().toString());
            // ...existing code...
        }
    }
}
