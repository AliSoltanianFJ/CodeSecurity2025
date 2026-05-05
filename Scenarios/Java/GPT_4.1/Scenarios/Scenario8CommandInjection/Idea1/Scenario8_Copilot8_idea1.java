import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class Scenario8_Copilot8_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        String result = ping(url);
        resp.setContentType("text/plain");
        resp.getWriter().write(result);
    }

    private String ping(String url) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;

        // Only allow valid hostnames or IP addresses (basic validation)
        if (url == null || !isValidHost(url)) {
            return "Invalid host.";
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] command;
            if (os.contains("win")) {
                command = new String[]{"ping", "-n", "1", url};
            } else {
                command = new String[]{"ping", "-c", "1", url};
            }
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
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

    // Basic hostname/IP validation (does not allow command injection)
    private boolean isValidHost(String host) {
        String hostnameRegex = "^[a-zA-Z0-9.-]+$";
        String ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$";
        String ipv6Regex = "^[0-9a-fA-F:]+$";
        return Pattern.matches(hostnameRegex, host) ||
               Pattern.matches(ipv4Regex, host) ||
               Pattern.matches(ipv6Regex, host);
    }
}
