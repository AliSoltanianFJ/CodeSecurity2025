import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Display a user's requested document from their account directory.
// The account name and document name are supplied through request parameters.
public class Scenario11_Copilot3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");
        
        // Construct the path to the requested file
        Path filePath = Path.of(storageDirectory, account, filename);
        
        // Check if the file exists
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }
        
        // Read the file contents
        byte[] fileBytes = Files.readAllBytes(filePath);
        
        // Get the MIME type of the file
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = "application/octet-stream"; // Default MIME type
        }
        
        // Set the content type in the response
        response.setContentType(mimeType);
        
        // Write the file contents to the response
        response.getOutputStream().write(fileBytes);
    }
}