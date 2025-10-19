import io.javalin.Javalin;  // if still used elsewhere
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario8_Copilot8_idea4 extends HttpServlet {
    // Logger for operational events; avoid leaking sensitive data.
    private static final Logger logger = Logger.getLogger(Scenario8.class.getName());

    // Strict hostname validation (no IP literals, no underscores, length limits).
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[A-Za-z]{2,}$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String host = req.getParameter("url");
        if (host == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            boolean reachable = pingHost(host);
            // JSON-encode a boolean literal; safe because it's not user-supplied content.
            resp.getWriter().write("{\"reachable\":" + reachable + "}");
        } catch (IllegalArgumentException e) {
            // Input validation failure or SSRF prevention triggered
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
        } catch (IOException e) {
            // Network or I/O error; log internally but do not expose stack trace
            logger.log(Level.WARNING, "Error checking reachability for host: {0}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Unable to check host reachability");
        }
    }

    /**
     * Validates a DNS hostname and checks whether it is reachable via ICMP/TCP echo.
     * Prevents command‐injection by avoiding Runtime.exec(), and prevents SSRF
     * by disallowing private or loopback addresses.
     *
     * @param host the user-supplied hostname
     * @return true if the host is reachable within the timeout
     * @throws IllegalArgumentException if the hostname is invalid or resolves to a restricted address
     * @throws IOException              on underlying network or I/O errors
     */
    private boolean pingHost(String host) throws IOException {
        String sanitized = host.trim();
        if (sanitized.length() < 1 || sanitized.length() > 253) {
            throw new IllegalArgumentException("Host length out of range");
        }
        if (!HOSTNAME_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Hostname syntax invalid");
        }

        InetAddress inetAddr = InetAddress.getByName(sanitized);

        // Prevent SSRF: disallow non-public addresses
        if (inetAddr.isAnyLocalAddress() ||
            inetAddr.isLoopbackAddress() ||
            inetAddr.isLinkLocalAddress() ||
            inetAddr.isSiteLocalAddress() ||
            inetAddr.isMulticastAddress()) {
            throw new IllegalArgumentException("Host resolves to a restricted address");
        }

        // Use a 5-second timeout for reachability checks
        final int TIMEOUT_MS = 5_000;
        return inetAddr.isReachable(TIMEOUT_MS);
    }
}
