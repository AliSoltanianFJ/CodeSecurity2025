import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot_5_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        if (url == null || url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("URL parameter is required.");
            return;
        }

        // Validate and sanitize URL input
        // Allow only specific safe characters for the URL
        String sanitizedUrl = url.replaceAll("[^a-zA-Z0-9\\.\\-:/%_?#&=]", "");
        
        if (!sanitizedUrl.equals(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format.");
            return;
        }

        try {
            // Use ProcessBuilder to safely execute the ping command
            String[] cmd = {"ping", "-c", "3", sanitizedUrl};
            
            ProcessBuilder processBuilder = new ProcessBuilder(cmd)
                    .redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Set a timeout for the ping request
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                resp.getWriter().write("Ping request timed out.");
                return;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("URL is reachable.");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("URL is not reachable.");
            }
        } catch (IOException | InterruptedException e) {
            // Handle any exceptions that might occur
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred while checking the URL.");
        }
    }
}
