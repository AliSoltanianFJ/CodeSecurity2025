import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.util.regex.Pattern;

public class Scenario4_Copilot_4_idea3 extends HttpServlet {
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
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

        if (filePart == null || filePart.getSize() == 0) {
            resp.getWriter().write("No file uploaded.");
            return;
        }

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        // Validate filename: only allow alphanumeric characters, underscores, and dots
        Pattern safePattern = Pattern.compile("^[a-zA-Z0-9_.]+$");
        if (!safePattern.matcher(fileName).matches()) {
            resp.getWriter().write("Invalid file name.");
            return;
        }

        // Ensure the file extension is .pdf
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            resp.getWriter().write("Only PDF files are allowed.");
            return;
        }

        String uploadDir = "./uploads";
        Path uploads = Paths.get(uploadDir);
        if (!Files.exists(uploads)) {
            Files.createDirectories(uploads);
        }

        // Generate a unique file name to prevent overwrites and potential directory traversal
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        Path dest = uploads.resolve(uniqueFileName);

        try (InputStream input = filePart.getInputStream();
             OutputStream output = Files.newOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        resp.getWriter().write("File uploaded successfully.");
    }
}