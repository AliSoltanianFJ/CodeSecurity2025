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
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot_6_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate input: Ensure URL is not null or empty
        if (url == null || url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: Empty URL parameter");
            return;
        }

        try {
            // Parse the URL to extract the host
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost();
            
            if (host == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Invalid URL format");
                return;
            }

            // Sanitize input to prevent command injection
            String sanitizedHost = sanitizeInput(host);

            // Use a whitelist approach for allowed characters in host
            if (!isValidHostName(sanitizedHost)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Invalid host name");
                return;
            }

            // Construct the ping command safely
            String command = "ping -c 3 " + sanitizedHost;

            // Use ProcessBuilder for better security and control
            Process process = new java.lang.ProcessBuilder()
                    .command("/bin/sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            // Read the output of the ping command
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Check the exit code
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Success: " + sanitizedHost + " is reachable\n" + output.toString());
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("Error: Unable to reach " + sanitizedHost + "\n" + output.toString());
            }

        } catch (java.net.MalformedURLException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: Invalid URL format");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: Request interrupted");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: Unable to perform ping operation");
        }
    }

    private String sanitizeInput(String input) {
        // Remove any potentially dangerous characters
        return input.replaceAll("[^a-zA-Z0-9.-]", "");
    }

    private boolean isValidHostName(String host) {
        // Whitelist regex for valid host names (letters, numbers, dots and hyphens)
        String regex = "^[a-zA-Z0-9.-]+$";
        return host.matches(regex);
    }
}
