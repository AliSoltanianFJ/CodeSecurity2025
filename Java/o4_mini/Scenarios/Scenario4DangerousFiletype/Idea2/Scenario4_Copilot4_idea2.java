import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Scenario4_Copilot4_idea2 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            // 1) ensure multipart
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            // 2) get part
            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }
            if (filePart == null) {
                resp.getWriter().write("No file part provided.");
                return;
            }

            // 3) sanitize filename
            String submittedFileName = Paths.get(filePart.getSubmittedFileName())
                    .getFileName()
                    .toString();
            if (submittedFileName.isEmpty() || !submittedFileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Invalid file extension. Only PDF files are allowed.");
                return;
            }

            // 4) size check
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.getWriter().write("File size exceeds the maximum allowed limit of 10 MB.");
                return;
            }

            // 5) content-type check
            String contentType = filePart.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                resp.getWriter().write("Invalid file content type.");
                return;
            }

            // 6) prepare uploads directory
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                resp.getWriter().write("Could not create upload directory.");
                return;
            }

            // 7) prevent overwriting
            Path targetFile = uploadDir.resolve(submittedFileName);
            if (Files.exists(targetFile)) {
                resp.getWriter().write("A file with that name already exists.");
                return;
            }

            // 8) validate PDF magic bytes and stream safely
            try (PushbackInputStream pbis = new PushbackInputStream(filePart.getInputStream(), 4);
                 OutputStream os = Files.newOutputStream(targetFile, 
                                 java.nio.file.StandardOpenOption.CREATE_NEW);
                 BufferedOutputStream bos = new BufferedOutputStream(os)) {

                byte[] header = new byte[4];
                int n = pbis.read(header);
                if (n < 4 || !new String(header, 0, 4, StandardCharsets.US_ASCII).equals("%PDF")) {
                    resp.getWriter().write("Uploaded file is not a valid PDF.");
                    Files.deleteIfExists(targetFile);
                    return;
                }
                pbis.unread(header, 0, n);

                byte[] buffer = new byte[8192];
                int len;
                while ((len = pbis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                bos.flush();
            } catch (IOException e) {
                resp.getWriter().write("File upload failed.");
                return;
            }

            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
