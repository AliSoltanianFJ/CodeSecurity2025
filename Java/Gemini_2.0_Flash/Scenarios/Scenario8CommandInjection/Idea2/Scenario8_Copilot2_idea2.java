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
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot2_idea2 extends HttpServlet {
    // Regex for validating IPv4, IPv6, or domain names (basic)
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$"
    );

    /**
     * Safely pings a host using the system "ping" command.
     * Only allows validated hostnames or IP addresses.
     * Returns the ping output or an error message.
     */
    public static String ping(String url) {
        if (url == null || url.length() > 255) {
            return "Invalid input.";
        }
        Matcher matcher = SAFE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return "Invalid host format.";
        }

        // Build the command safely
        String[] cmd = {"ping", "-n", "2", url}; // Windows: -n, Linux: -c

        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;
        try {
            process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null && linesRead < 20) { // Prevent buffer overflow
                // Basic sanitization to prevent XSS in output
                output.append(line.replaceAll("[<>]", "")).append("\n");
                linesRead++;
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Ping failed with exit code ").append(exitCode).append("\n");
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing ping: ").append(e.getMessage()).append("\n");
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {}
            if (process != null) process.destroy();
        }
        return output.toString();
    }

    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        String result = ping(url);

        // Set content type and prevent XSS
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.getWriter().write(result);
    }
}
