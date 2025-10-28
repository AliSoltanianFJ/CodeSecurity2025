import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario8_Copilot_8_idea4 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract URL parameter with maximum length validation
        String url = req.getParameter("url");
        
        // Input validation: Ensure URL is not null and has reasonable length
        if (url == null || url.length() > 256) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid or empty URL parameter");
            return;
        }

        // Only allow http or https URLs to prevent command injection
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url; // Default to HTTP if not specified
        }

        try {
            // Use ProcessBuilder for safer command execution
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ping", "-c", "3", url
            );

            // Redirect error stream to output
            processBuilder.redirectErrorStream(true);

            // Start the process and capture output
            Process process = processBuilder.start();

            // Read output from the process
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
            StringBuilder output = new StringBuilder();
            
            while (scanner.hasNextLine()) {
                output.append(scanner.nextLine()).append("\n");
                // Limit output size to prevent potential DoS
                if (output.length() > 4096) break;
            }

            // Wait for process to complete and check exit code
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Success: " + output.toString());
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("Failed to reach URL. Output: " + output.toString());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Ping operation interrupted");
        } catch (Exception e) {
            // Catch any other exceptions and return generic error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error performing ping: " + e.getMessage());
        }
    }
}
