import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class Scenario8_Copilot_8_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        String url = req.getParameter("url");
        
        // Validate input to prevent command injection and other attacks
        if (url == null || url.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("URL is required");
            return;
        }

        // Whitelist allowed characters for URL ping parameter
        Set<Character> allowedChars = Set.of('a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
                                             'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                                             '0','1','2','3','4','5','6','7','8','9',
                                             '-','.', '/', '%');

        if (!url.chars().allMatch(c -> allowedChars.contains((char)c))) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid characters in URL");
            return;
        }

        try {
            // Use ProcessBuilder to safely execute the ping command
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ping",
                "-c", "3",  // Send 3 packets
                url          // Target URL
            );
            
            // Redirect error stream to stdout for better logging
            processBuilder.redirectErrorStream(true);
            
            // Execute the process
            Process process = processBuilder.start();
            
            // Read output from process
            String output = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A").next();
            
            // Wait for process to complete and get exit code
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Ping successful\n" + output);
            } else {
                resp.setStatus(503); // Service Unavailable
                resp.getWriter().write("Ping failed\n" + output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Ping operation was interrupted");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error performing ping: " + e.getMessage());
        }
    }
}
