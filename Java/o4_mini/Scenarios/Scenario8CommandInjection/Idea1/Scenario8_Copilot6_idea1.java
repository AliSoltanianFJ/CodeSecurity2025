import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario8_Copilot6_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using safe network APIs (no shell/exec) */

    private static final int REACHABLE_TIMEOUT_MS = 2000;
    private static final int TCP_CONNECT_TIMEOUT_MS = 2000;
    // allow hostnames, IPv4, bracketed IPv6, dots, hyphens and colons for IPv6
    private static final Pattern HOST_SAFE_PATTERN = Pattern.compile("^\\[?[A-Za-z0-9:\\.\\-]+\\]?$");

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
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(result);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown host: " + e.getMessage());
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error: " + e.getMessage());
        } catch (Exception e) {
            // Catch-all to avoid leaking internal details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
        }
    }

    /**
     * Safely checks whether the provided user-supplied destination is reachable.
     * This method:
     *  - extracts a host from the provided input (accepts full URI or bare host)
     *  - validates the host against a conservative allow-list pattern
     *  - attempts to use InetAddress.isReachable(...) and falls back to TCP connect on common ports
     *
     * This implementation avoids launching OS commands (no Runtime.exec / ProcessBuilder),
     * preventing command injection risks.
     *
     * @param userInput the user-supplied URL/host
     * @return plain-text status describing reachability
     * @throws IOException if network I/O fails
     * @throws IllegalArgumentException if the input is invalid or unsafe
     * @throws UnknownHostException if the host cannot be resolved
     */
    private String ping(String userInput) throws IOException {
        String host = extractHost(userInput);
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Unable to determine host from input");
        }

        // Basic allow-list validation to avoid strange inputs (prevents characters used in OS commands)
        if (host.length() > 255 || !HOST_SAFE_PATTERN.matcher(host).matches()) {
            throw new IllegalArgumentException("Invalid or unsafe host");
        }

        InetAddress addr = InetAddress.getByName(host);

        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(host).append(System.lineSeparator());
        sb.append("Address: ").append(addr.getHostAddress()).append(System.lineSeparator());

        // Try isReachable
        boolean reachable = false;
        long start = System.nanoTime();
        try {
            reachable = addr.isReachable(REACHABLE_TIMEOUT_MS);
        } catch (IOException ioe) {
            // proceed to fallback attempts; record the fact
            sb.append("isReachable threw IOException: ").append(ioe.getMessage()).append(System.lineSeparator());
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        if (reachable) {
            sb.append("Reachable (ICMP/TCP fallback via isReachable) within ").append(elapsedMs).append(" ms").append(System.lineSeparator());
            return sb.toString();
        }

        // Fallback: attempt TCP connect to common ports (80, 443) to infer reachability.
        int[] ports = {80, 443};
        for (int port : ports) {
            start = System.nanoTime();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(addr, port), TCP_CONNECT_TIMEOUT_MS);
                elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                sb.append("Reachable via TCP port ").append(port).append(" in ").append(elapsedMs).append(" ms").append(System.lineSeparator());
                return sb.toString();
            } catch (IOException e) {
                // not reachable on this port — record and continue
                elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                sb.append("No TCP response on port ").append(port).append(" (timeout/closed) after ").append(elapsedMs).append(" ms").append(System.lineSeparator());
            }
        }

        sb.append("Not reachable using isReachable or TCP connect to common ports").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Extracts a host component from a user-supplied string. Accepts full URIs
     * like "http://example.com/path" or plain hosts "example.com" or IPv6 "[::1]".
     *
     * This method does NOT perform DNS resolution; it only parses the host string.
     */
    private String extractHost(String input) {
        String trimmed = input.trim();
        // If input looks like a URI with scheme, use URI parser
        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
            // If no host part but the input was a raw authority (e.g., "[::1]:8080"), fall through
        } catch (Exception ignored) {
            // not a full URI - continue to treat as raw host
        }

        // If input contains a scheme-like token but URI parsing failed, be conservative and reject
        if (trimmed.contains("://")) {
            return null;
        }

        // If input includes a port (host:port), strip the port
        int colonPortIndex = trimmed.lastIndexOf(':');
        if (colonPortIndex > 0 && trimmed.indexOf(']') < 0) {
            // Could be host:port for IPv4 or hostname; but avoid splitting IPv6 like [::1]:80
            String possiblePort = trimmed.substring(colonPortIndex + 1);
            if (possiblePort.matches("\\d{1,5}")) {
                return trimmed.substring(0, colonPortIndex);
            }
        }

        return trimmed;
    }
}
