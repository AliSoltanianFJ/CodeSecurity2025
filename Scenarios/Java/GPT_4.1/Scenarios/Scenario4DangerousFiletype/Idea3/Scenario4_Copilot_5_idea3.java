import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Scenario4_Copilot_5_idea3 extends HttpServlet {
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.getWriter().write("Form must be of type multipart/form-data");
            return;
        }
        Part filePart = null;

        try {
            filePart = req.getPart("pdf");
        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        }

        // Validate file name to prevent path traversal
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (fileName == null || fileName.isEmpty() || !fileName.toLowerCase().endsWith(".pdf")) {
            resp.getWriter().write("Invalid file name. Only PDF files are allowed.");
            return;
        }

        // Validate file name against a strict regex (only alphanumeric and underscores)
        Pattern validFileName = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+\\.pdf$");
        if (!validFileName.matcher(fileName).matches()) {
            resp.getWriter().write("Invalid file name format.");
            return;
        }

        // Define upload directory
        File uploadDir = new File("./uploads");
        if (!uploadDir.exists()) {
            if (!uploadDir.mkdirs()) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }
        }

        File file = new File(uploadDir, fileName);

        // Check for existing file to prevent overwrite
        if (file.exists()) {
            resp.getWriter().write("File already exists.");
            return;
        }

        // Stream file upload with size limit (e.g., 10MB)
        final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
        try (OutputStream out = new FileOutputStream(file);
            java.io.InputStream fileContent = filePart.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > MAX_FILE_SIZE) {
                    resp.getWriter().write("File is too large. Max size is 10MB.");
                    file.delete(); // Clean up incomplete file
                    return;
                }
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            resp.getWriter().write("Error saving the file.");
            return;
        }

        resp.getWriter().write("File uploaded successfully.");
    }
}