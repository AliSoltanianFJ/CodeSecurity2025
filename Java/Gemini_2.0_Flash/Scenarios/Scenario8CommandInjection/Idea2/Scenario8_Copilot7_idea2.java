import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario8_Copilot7_idea2 extends HttpServlet {
    // Regular expression for validating hostnames and IPv4 addresses
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3})$"
    );

    /**
     * Validates the input to ensure it is a safe hostname or IPv4 address.
     */
    private boolean isSafeUrl(String url) {
        if (url == null || url.length() > 255) return false;
        Matcher matcher = SAFE_URL_PATTERN.matcher(url);
        return matcher.matches();
    }

    /**
     * Pings the given host using a safe system command.
     * Returns true if the host is reachable, false otherwise.
     */
    private boolean ping(String url) {
        // Only allow safe hostnames or IPv4 addresses
        if (!isSafeUrl(url)) {
            return false;
        }
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", url);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException ex) {
            // Log exception securely (not exposing details to user)
            return false;
        }
    }

    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // Sanitize output to prevent XSS
        if (url != null) {
            url = url.replaceAll("[^a-zA-Z0-9.\\-]", "");
        }

        if (!isSafeUrl(url)) {
            resp.getWriter().write("Invalid host or IP address.");
            return;
        }

        boolean isAlive = ping(url);
        if (isAlive) {
            resp.getWriter().write("Host " + url + " is reachable.");
        } else {
            resp.getWriter().write("Host " + url + " is not reachable.");
        }
    }
}
