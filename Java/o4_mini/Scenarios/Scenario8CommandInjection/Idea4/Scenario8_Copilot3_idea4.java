import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Securely checks reachability of a given host by name or IP.
 * - Validates and sanitizes input URL/host
 * - Prevents SSRF by blocking private/internal addresses
 * - Uses InetAddress.isReachable instead of shelling out
 * - Requires HTTPS
 * - Returns JSON without leaking internals
 */
public class Scenario8_Copilot3_idea4 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario8.class.getName());
    private static final int TIMEOUT_MS = 5000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce TLS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
            return;
        }

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        URI uri;
        try {
            uri = new URI(urlParam.trim());
        } catch (URISyntaxException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed URL");
            return;
        }

        String scheme = uri.getScheme();
        if (scheme != null
                && !scheme.equalsIgnoreCase("http")
                && !scheme.equalsIgnoreCase("https")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported URL scheme");
            return;
        }

        String host = uri.getHost();
        if (host == null || !host.matches("^[a-zA-Z0-9.-]{1,253}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        boolean reachable;
        try {
            reachable = pingHost(host, TIMEOUT_MS);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            return;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error pinging host: " + host, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String json = String.format("{\"host\":\"%s\",\"reachable\":%b}", host, reachable);
        resp.getWriter().write(json);
    }

    /**
     * Pings a host with a timeout, blocking internal/private addresses.
     *
     * @param host    DNS name or literal IP
     * @param timeout timeout in ms
     * @return true if host replies to ping within timeout
     * @throws IOException              on network errors
     * @throws IllegalArgumentException on forbidden internal addresses
     */
    private boolean pingHost(String host, int timeout) throws IOException {
        InetAddress address = InetAddress.getByName(host);

        // Block access to internal networks (SSRF mitigation)
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()) {
            throw new IllegalArgumentException("Access to internal addresses is forbidden");
        }

        return address.isReachable(timeout);
    }
}
