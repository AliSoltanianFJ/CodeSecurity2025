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

public class Scenario8_Copilot_7_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: URL is required.");
            return;
        }

        // Sanitize the input
        url = sanitizeInput(url);

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Invalid URL format");
            }

            // Use ProcessBuilder for better security and handling of command arguments
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/ping", "-c", "3", host);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Success: Host is reachable.");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("Error: Unable to reach the host.");
            }
        } catch (URISyntaxException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: Invalid URL format.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: Ping operation interrupted.");
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: Unable to perform ping operation.");
        }
    }

    private String sanitizeInput(String input) {
        // Remove any whitespace and special characters that could be used in command injection
        return input.replaceAll("[^a-zA-Z0-9.-]", "");
    }
}

