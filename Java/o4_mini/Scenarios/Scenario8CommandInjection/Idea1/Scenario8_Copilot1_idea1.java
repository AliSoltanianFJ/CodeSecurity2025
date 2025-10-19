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

public class Scenario8_Copilot1_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using a safe Java API (no shell execution) */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // ...existing code...

        resp.setContentType("text/plain; charset=UTF-8");
        if (url == null || url.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter.");
            return;
        }

        try {
            String result = ping(url.trim());
            resp.getWriter().write(result);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Host not found: " + e.getMessage());
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error while checking host: " + e.getMessage());
        }
    }

    // ...existing code...

    /**
     * Perform a safe "ping" using InetAddress.isReachable rather than executing a system command.
     * Validates the input hostname to reduce the chance of misuse.
     *
     * @param host host name or IP address supplied by user
     * @return textual result describing reachability
     * @throws IOException for network/lookup errors
     * @throws IllegalArgumentException for invalid input
     * @throws UnknownHostException if the host cannot be resolved
     */
    private String ping(String host) throws IOException {
        if (host == null) {
            throw new IllegalArgumentException("Host is null");
        }
        host = host.trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Host is empty");
        }
        if (host.length() > 255) {
            throw new IllegalArgumentException("Host name too long");
        }
        // Prevent whitespace/control characters
        if (host.matches(".*\\s+.*")) {
            throw new IllegalArgumentException("Invalid host (contains whitespace)");
        }
        // Only allow typical hostname / IP characters: letters, digits, dot, hyphen, colon, percent, brackets (for IPv6)
        if (!host.matches("^[A-Za-z0-9.\\-:%\\[\\]]+$")) {
            throw new IllegalArgumentException("Invalid characters in host");
        }

        InetAddress addr = InetAddress.getByName(host); // may throw UnknownHostException
        int timeoutMs = 5000;
        boolean reachable = addr.isReachable(timeoutMs); // uses ICMP or TCP fallback, no shell

        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(addr.getHostName())
          .append(" (").append(addr.getHostAddress()).append(")").append(System.lineSeparator());
        sb.append("Reachable within ").append(timeoutMs).append(" ms: ").append(reachable);
        return sb.toString();
    }
}
