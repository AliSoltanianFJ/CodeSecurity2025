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
import java.net.URL;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Scenario8_Copilot3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing 'url' parameter");
            return;
        }

        String result = ping(url);
        resp.setContentType("text/plain");
        resp.getWriter().write(result);
    }

    /**
     * Perform a safe ping to the provided target.
     * - Accepts either a hostname/IP or a full URL (http(s)://...).
     * - Validates and sanitizes the target to avoid command injection.
     * - Uses ProcessBuilder with argument list (no shell) and a short timeout.
     */
    private String ping(String target) {
        if (target == null) {
            return "Invalid target: null";
        }

        // If a full URL was provided, extract the host part
        String host = target.trim();
        try {
            URL u = new URL(host);
            String h = u.getHost();
            if (h != null && !h.isEmpty()) {
                host = h;
            }
        } catch (MalformedURLException e) {
            // not a full URL treat the provided string as host
        }

        // Basic validation: allow only letters, digits, dots and hyphens (no spaces, no shell metachars)
        Pattern hostPattern = Pattern.compile("^[A-Za-z0-9.-]{1,255}$");
        if (!hostPattern.matcher(host).matches()) {
            return "Invalid host: contains disallowed characters";
        }

        // Determine OS-specific ping arguments
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String[] cmd;
        if (isWindows) {
            cmd = new String[] { "ping", "-n", "1", host };
        } else {
            cmd = new String[] { "ping", "-c", "1", host };
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        StringBuilder out = new StringBuilder();
        Process proc = null;
        try {
            proc = pb.start();

            // Read output
            try (BufferedReader rdr = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = rdr.readLine()) != null) {
                    out.append(line).append(System.lineSeparator());
                }
            }

            // Wait with timeout
            boolean finished = proc.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                return "Ping timed out";
            }

            int exit = proc.exitValue();
            if (exit == 0) {
                return "Host reachable\n\n" + out.toString();
            } else {
                return "Host not reachable (exit code " + exit + ")\n\n" + out.toString();
            }
        } catch (IOException e) {
            return "IO error while running ping: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Ping interrupted";
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }
}
