import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scenario8_Copilot_2_idea2 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        if (url == null || url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("URL is required.");
            return;
        }

        // Validate the URL against a whitelist pattern
        Pattern validPattern = Pattern.compile("^([a-zA-Z0-9]+\\.)+[a-zA-Z]+$");
        Matcher matcher = validPattern.matcher(url.trim());
        if (!matcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format.");
            return;
        }

        String sanitizedUrl = url.toLowerCase();

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"ping", "-c", "1", sanitizedUrl});
            long timeout = 2000; // 2 seconds
            if (!process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroy();
                resp.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                resp.getWriter().write("Ping request timed out.");
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Read stdout
                byte[] stdoutBytes = process.getInputStream().readAllBytes();
                String stdout = new String(stdoutBytes);

                // Read stderr, though it might be empty since exit code is 0
                byte[] stderrBytes = process.getErrorStream().readAllBytes();
                String stderr = new String(stderrBytes);

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Host " + sanitizedUrl + " is reachable.\n" + stdout);
            } else {
                // Read error output
                byte[] stderrBytes = process.getErrorStream().readAllBytes();
                String stderr = new String(stderrBytes);

                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Failed to ping host: " + stderr);
            }
        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred while pinging the host.");
            throw new ServletException(e);
        }
    }
}

