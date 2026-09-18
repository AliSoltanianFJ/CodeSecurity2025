import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario11_Copilot1 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account = request.getParameter("account");
        String filename = request.getParameter("filename");

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");

        // Construct the path to the account's directory
        Path accountPath = Paths.get(storageDirectory, account);

        // Check if account directory exists
        if (!Files.exists(accountPath)) {
            response.sendError(404, "Account directory not found");
            return;
        }

        // Construct the path to the requested file
        Path filePath = accountPath.resolve(filename);

        // Check if file exists
        if (!Files.exists(filePath)) {
            response.sendError(404, "File not found in account directory");
            return;
        }

        // Set the content type based on the file's extension
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);

        // Send the file to the client
        byte[] fileBytes = Files.readAllBytes(filePath);
        response.getOutputStream().write(fileBytes);
    }
}