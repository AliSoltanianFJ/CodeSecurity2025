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

public class Scenario8_Copilot_8_idea3 extends HttpServlet {
    private static final Pattern SAFE_HOST_PATTERN = Pattern.compile("^(https?://)?([a-zA-Z0-9.-]+)$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        String result = ping(url);
        resp.setContentType("text/plain");
        resp.getWriter().write(result);
    }

    /**
     * Safely pings a host or URL, avoiding command injection.
     * Only allows hostnames or IP addresses, no shell metacharacters.
     */
    private String ping(String url) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;

        // Only allow hostnames or IPv4 addresses
        if (url == null || url.length() > 253 || !isValidHostOrIp(url)) {
            return "Invalid host or IP address.";
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            if (os.contains("win")) {
                cmd = new String[]{"ping", "-n", "1", url};
            } else {
                cmd = new String[]{"ping", "-c", "1", url};
            }
            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            int exitCode = process.waitFor();
            output.append("Exit code: ").append(exitCode);
        } catch (IOException | InterruptedException e) {
            output.append("Error: ").append(e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    output.append("Error closing reader: ").append(e.getMessage());
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }

    /**
     * Validates that the input is a hostname or IPv4 address.
     * Rejects anything else to prevent command injection.
     */
    private boolean isValidHostOrIp(String input) {
        // Validate IPv4
        if (input.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            String[] parts = input.split("\\.");
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        }
        // Validate hostname (RFC 1123)
        if (input.matches("^[a-zA-Z0-9-]{1,63}(\\.[a-zA-Z0-9-]{1,63})*$")) {
            return true;
        }
        return false;
    }
}