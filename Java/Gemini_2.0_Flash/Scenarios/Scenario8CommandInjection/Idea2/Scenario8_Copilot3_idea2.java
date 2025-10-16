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

public class Scenario8_Copilot3_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        // Validate and sanitize input: Only allow IPv4/IPv6 addresses or domain names
        if (url == null || url.length() > 253) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input.");
            return;
        }

        // Only allow valid domain names or IP addresses (basic check)
        Pattern pattern = Pattern.compile("^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format.");
            return;
        }

        // Build command safely, no shell interpretation
        String[] command = {"ping", "-n", "1", url};

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 10) { // Prevent buffer overflow
                    output.append(line.replaceAll("[<>]", "")).append("\n"); // Prevent XSS
                    linesRead++;
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Ping successful:\n" + output.toString());
            } else {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Ping failed:\n" + output.toString());
            }
        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error executing ping.");
        }
    }
}
