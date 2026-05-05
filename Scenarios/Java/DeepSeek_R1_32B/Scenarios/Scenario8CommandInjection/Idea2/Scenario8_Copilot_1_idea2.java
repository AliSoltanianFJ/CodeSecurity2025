import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;
import java.net.URISyntaxException;

public class Scenario8_Copilot_1_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        if (url == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("URL parameter is required.");
            return;
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            // Validate the host to prevent command injection
            if (!isValidHost(host)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid URL host.");
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ping", "-c", "3", host)
                    .redirectErrorStream(true);
            Process process = null;
            try {
                process = processBuilder.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    resp.getWriter().write("Host is alive.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    resp.getWriter().write("Could not reach the host.");
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while pinging the host.");
            }

        } catch (IllegalArgumentException | URISyntaxException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format: " + e.getMessage());
        }
    }

    private boolean isValidHost(String host) {
        // Allow letters, numbers, dots, hyphens, and colons (for IPv6)
        String pattern = "^([a-zA-Z0-9.-]+|\\[[a-fA-F0-9:]+\\])$";
        return Pattern.matches(pattern, host);
    }
}

