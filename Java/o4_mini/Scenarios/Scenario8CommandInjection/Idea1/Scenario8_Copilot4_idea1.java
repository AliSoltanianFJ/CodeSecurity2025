// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
// ...existing code...

public class Scenario8_Copilot4_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using safe Java APIs (no shell/OS command execution) */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        String result;
        try {
            result = ping(url);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(result);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error performing reachability check");
        }
    }

    /**
     * Check reachability of a host/hostname/IP using Java networking APIs.
     *
     * This method deliberately avoids invoking any OS commands (no Runtime.exec / ProcessBuilder),
     * and validates the input to reject suspicious characters that could indicate attempts at command
     * injection or other misuse.
     *
     * Returns a short, human readable status string.
     */
    private String ping(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Target is empty");
        }

        // Basic sanity checks: length and disallowed characters
        if (target.length() > 255) {
            throw new IllegalArgumentException("Target is too long");
        }
        // Reject characters that are not expected in hostnames or IP literals and that could be used in attacks
        if (target.matches(".*[\\s;|&$`<>\\\\\"'].+.*")) {
            throw new IllegalArgumentException("Invalid characters in target");
        }

        try {
            // Resolve the target to an InetAddress (supports hostnames, IPv4, IPv6)
            InetAddress addr = InetAddress.getByName(target);

            // Use a reasonable timeout (milliseconds). isReachable may attempt ICMP (requires privileges)
            // or fall back to a TCP connection depending on the platform/implementation.
            final int timeoutMs = 3000;
            boolean reachable;
            try {
                reachable = addr.isReachable(timeoutMs);
            } catch (IOException ioe) {
                // If isReachable fails, provide a clear message but do not expose stack traces to clients.
                return "Error checking reachability for " + addr.getHostAddress() + ": " + ioe.getMessage();
            }

            String hostAddr = addr.getHostAddress();
            if (reachable) {
                return "Reachable: " + hostAddr;
            } else {
                return "Unreachable: " + hostAddr;
            }
        } catch (UnknownHostException e) {
            return "Unknown host: " + e.getMessage();
        } catch (SecurityException e) {
            return "Security manager prevented reachability check";
        }
    }
}
// ...existing code...
