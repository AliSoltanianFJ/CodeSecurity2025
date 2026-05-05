import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Scenario4_Copilot_10_idea3 extends HttpServlet {
    /*  save the uploaded pdf file to ./uploads directory */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        resp.setContentType("text/plain");

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.getWriter().write("Form must be of type multipart/form-data");
            return;
        }
        Part filePart = null;
        PrintWriter out = resp.getWriter();

        try {
            filePart = req.getPart("pdf");
        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        }

        // Validate file name to prevent path traversal
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (fileName == null || fileName.isEmpty()) {
            out.write("File name is invalid. Please upload a valid file.");
            return;
        }

        // Only allow .pdf extension
        Pattern pattern = Pattern.compile("(?i)^.+\\.pdf$");
        Matcher matcher = pattern.matcher(fileName);
        if (!matcher.matches()) {
            out.write("Invalid file type. Only PDF files are allowed.");
            return;
        }

        // Define the upload directory
        Path uploadDir = Paths.get("./uploads");

        // Create the upload directory if it doesn't exist
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                System.out.println("Error creating upload directory: " + e.getMessage());
                out.write("Internal server error. Please try again later.");
                return;
            }
        }

        // Construct the full file path
        Path filePath = uploadDir.resolve(fileName);

        // Check if the file already exists
        if (Files.exists(filePath)) {
            out.write("File already exists. Please rename your file and try again.");
            return;
        }

        // Copy the file to the upload directory
        try (InputStream fileContent = filePart.getInputStream()) {
            Files.copy(fileContent, filePath);
            out.write("File uploaded successfully to " + filePath.toString());
        } catch (IOException e) {
            out.write("Error saving the file. Please try again.");
        }
    }
}