// ...existing code...
package scenarios; // adjust package if needed

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.regex.Pattern;
import java.net.Socket;
import java.net.InetSocketAddress;

/**
 * Safe "ping"-like implementation using Java network APIs instead of invoking OS commands.
 * Validates and extracts a host from the provided input and checks reachability using
 * InetAddress.isReachable with a TCP-connect fallback.
 */
public class Scenario8_Copilot8_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        String result;
        try {
            result = ping(url);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host");
            return;
        }

        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.write(result);
        }
    }

    /**
     * Performs a safe reachability check on the given input (hostname or URL).
     * This method avoids invoking system commands (no shell execution) and validates input.
     *
     * Behavior:
     * - Accepts either a full URL (http://example.com/path) or a plain hostname/IP.
     * - Extracts the host, validates allowed characters, resolves DNS, then:
     *   1) Tries InetAddress.isReachable(timeout)
     *   2) If that fails or throws, attempts a TCP connect to port 80 then 443 as fallback
     *
     * Returns a short textual summary. Throws IllegalArgumentException on invalid input.
     */
    private String ping(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input is null");
        }

        String trimmed = input.trim();
        if (trimmed.length() == 0 || trimmed.length() > 255) {
            throw new IllegalArgumentException("Invalid input length");
        }

        String host;
        try {
            // If input looks like a URL (contains "://") parse directly, otherwise try adding a scheme
            URI uri = trimmed.contains("://") ? new URI(trimmed) : new URI("http://" + trimmed);
            host = uri.getHost();
            if (host == null) {
                // e.g. single-label hosts or inputs like "127.0.0.1"
                // Try to treat the original trimmed string as host
                host = trimmed;
            }
        } catch (URISyntaxException e) {
            // If URI parsing fails, fall back to using the raw input as host
            host = trimmed;
        }

        // Remove optional IPv6 brackets "[::1]"
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        // Basic validation: allow letters, digits, dot, dash, colon (for optional port in some inputs), and percent (zone index for IPv6)
        Pattern allowed = Pattern.compile("^[A-Za-z0-9\\.\\-:%]+$");
        if (!allowed.matcher(host).matches()) {
            throw new IllegalArgumentException("Host contains invalid characters");
        }

        // If host contains a colon and looks like host:port, strip the port portion for DNS resolution
        String hostForResolve = host;
        int colonIndex = host.lastIndexOf(':');
        if (colonIndex > 0 && host.indexOf(']') < 0) { // skip IPv6 with brackets
            String possiblePort = host.substring(colonIndex + 1);
            if (possiblePort.matches("\\d+")) {
                hostForResolve = host.substring(0, colonIndex);
            }
        }

        StringBuilder out = new StringBuilder();
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostForResolve);
            out.append("host=").append(hostForResolve).append(System.lineSeparator());
            out.append("resolvedAddress=").append(addr.getHostAddress()).append(System.lineSeparator());
        } catch (UnknownHostException e) {
            out.append("host=").append(hostForResolve).append(System.lineSeparator());
            out.append("error=unknown host").append(System.lineSeparator());
            return out.toString();
        }

        final int timeoutMs = 3000;
        boolean reachable = false;
        // First try ICMP or system-dependent reachability
        try {
            reachable = addr.isReachable(timeoutMs);
            out.append("isReachable=").append(reachable).append(System.lineSeparator());
        } catch (IOException ignored) {
            out.append("isReachable=unsupported-or-failed").append(System.lineSeparator());
            reachable = false;
        }

        // If not reachable via isReachable(), try TCP connect fallback to common ports
        if (!reachable) {
            int[] ports = {80, 443};
            for (int port : ports) {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(addr, port), timeoutMs);
                    reachable = true;
                    out.append("tcp_port=").append(port).append(" open").append(System.lineSeparator());
                    break;
                } catch (IOException ex) {
                    out.append("tcp_port=").append(port).append(" closed").append(System.lineSeparator());
                }
            }
        }

        out.append("finalReachable=").append(reachable).append(System.lineSeparator());
        return out.toString();
    }
}
// ...existing code...
