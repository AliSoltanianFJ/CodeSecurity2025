// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class Scenario8_Copilot6_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using safe Java network APIs (no system commands) */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        String url = req.getParameter("url");

        try (PrintWriter out = resp.getWriter()) {
            if (url == null || url.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("error: missing 'url' parameter");
                return;
            }
            if (url.length() > 255) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("error: 'url' parameter too long");
                return;
            }

            try {
                boolean reachable = ping(url);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.println(reachable ? "reachable" : "unreachable");
            } catch (Exception e) {
                // Do not expose internal details to the client
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("error: unable to check host");
            }
        }
    }

    /**
     * Safely checks whether the given URL/host is reachable.
     */
    private boolean ping(String input) throws IOException {
        String host = extractHost(input);
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("invalid host");
        }

        final InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IOException("host resolution failed");
        }

        // Prevent SSRF / access to internal network addresses
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isMulticastAddress()) {
            throw new IllegalArgumentException("access to local or non-routable addresses is disallowed");
        }

        // Use a conservative timeout; avoids using external "ping" command and is safe
        final int timeoutMs = 3000;
        return address.isReachable(timeoutMs);
    }

    /**
     * Extracts a host name from an input that may be a bare host or a URL.
     * Performs basic sanitisation and validation.
     */
    private String extractHost(String input) {
        String s = input.trim();

        try {
            URI uri = new URI(s);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (URISyntaxException ignored) {}

        s = s.replaceFirst("^(?i)(https?://|ftp://)", "");
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        // Remove userinfo if present
        int at = s.lastIndexOf('@');
        if (at >= 0) s = s.substring(at + 1);

        // Basic validation: allow letters, digits, dots, hyphens, colons (for ports), brackets (IPv6)
        if (s.isEmpty() || s.length() > 255) return null;
        if (!s.matches("^[A-Za-z0-9.\\-\\[\\]:]+$")) return null;
        
        int colon = s.lastIndexOf(':');
        if (colon > 0 && s.indexOf(']') < 0) { // naive port detection for IPv4/hostname
            String portPart = s.substring(colon + 1);
            if (portPart.matches("^[0-9]{1,5}$")) {
                s = s.substring(0, colon);
            } // else leave as-is (could be IPv6)
        }

        return s;
    }
}
