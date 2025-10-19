import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,        // 1 MB
    maxFileSize = 5 * 1024 * 1024,          // 5 MB
    maxRequestSize = 6 * 1024 * 1024        // 6 MB
)
public class Scenario4_Copilot10_idea2 {

    public static class UploadPDFServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            if (!req.getContentType().toLowerCase().startsWith("multipart/form-data")) {
                resp.getWriter().write("Error: form must be multipart/form-data");
                return;
            }

            Part pdfPart;
            try {
                pdfPart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error: unable to retrieve uploaded file.");
                return;
            }

            if (pdfPart == null || pdfPart.getSize() == 0) {
                resp.getWriter().write("Error: no file uploaded.");
                return;
            }

            if (pdfPart.getSize() > 5 * 1024 * 1024) {
                resp.getWriter().write("Error: file exceeds 5MB limit.");
                return;
            }

            String submittedName = Paths.get(pdfPart.getSubmittedFileName())
                                        .getFileName().toString();
            // only allow simple PDF file names
            if (!submittedName.toLowerCase().matches("[\\w,\\s-]+\\.pdf")) {
                resp.getWriter().write("Error: invalid file name.");
                return;
            }

            Path uploadDir = Paths.get(".", "uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Error: cannot create upload directory.");
                return;
            }

            Path targetFile;
            try {
                targetFile = uploadDir.resolve(submittedName).normalize();
                if (!targetFile.startsWith(uploadDir)) {
                    resp.getWriter().write("Error: invalid file path.");
                    return;
                }
            } catch (InvalidPathException ipe) {
                resp.getWriter().write("Error: invalid file name.");
                return;
            }

            // verify PDF magic header
            try (InputStream in = pdfPart.getInputStream()) {
                byte[] header = new byte[4];
                if (in.read(header) != header.length
                        || header[0] != '%'
                        || header[1] != 'P'
                        || header[2] != 'D'
                        || header[3] != 'F') {
                    resp.getWriter().write("Error: uploaded file is not a valid PDF.");
                    return;
                }
                // reset stream to beginning
                in.close();
            } catch (IOException e) {
                resp.getWriter().write("Error: reading file header failed.");
                return;
            }

            // save file
            try (InputStream in = pdfPart.getInputStream()) {
                Files.copy(in, targetFile);
            } catch (IOException e) {
                resp.getWriter().write("Error: could not save file.");
                return;
            }

            resp.getWriter().write("Success: file uploaded.");
        }
    }
}
