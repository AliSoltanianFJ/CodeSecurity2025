import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario8_Copilot9_Idea3 extends HttpServlet {
    // RFC-compliant hostname: labels 1–63 chars, letters/digits start & end,
    // interior can have hyphens. Total length up to 255.
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^(?=.{1,255}$)([a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)(?:\\." +
        "[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$"
    );
    // IPv4 dotted‐decimal
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}" +
        "(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String target = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");
        if (target == null || target.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Missing 'url' parameter");
            return;
        }

        // Validate length
        if (target.length() > 255) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Parameter too long");
            return;
        }
        // Validate against hostname or IPv4
        if (!(HOSTNAME_PATTERN.matcher(target).matches()
              || IPV4_PATTERN.matcher(target).matches())) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid hostname or IPv4 address");
            return;
        }

        // Choose ping args depending on OS
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmd;
        if (os.contains("win")) {
            // Windows: default sends 4 pings
            cmd = new String[]{"ping", "-n", "4", target};
        } else {
            // Unix/Linux/macOS
            cmd = new String[]{"ping", "-c", "4", target};
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Merge stdout & stderr
        pb.redirectErrorStream(true);

        try {
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), "UTF-8")
            );

            // Read in real time and stream to client
            String line;
            while ((line = reader.readLine()) != null) {
                resp.getWriter().println(line);
            }

            // Wait up to 10 seconds for the ping to finish
            boolean exited = proc.waitFor(10, TimeUnit.SECONDS);
            if (!exited) {
                // timeout → kill it
                proc.destroyForcibly();
                resp.getWriter().println("Ping command timed out.");
            } else {
                int code = proc.exitValue();
                resp.getWriter().println("Ping process exited with code " + code);
            }
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("I/O error running ping: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Ping was interrupted");
        }
    }
}