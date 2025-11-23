// ...existing code...
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

public class Scenario8_Copilot9_idea2 extends HttpServlet {
    private static final int MAX_URL_LENGTH = 2083;
    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.-]{1,255}$");
    private static final int DEFAULT_TIMEOUT_MS = 3000;

    /* ping a URL to see if it is alive without using system commands */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String url = req.getParameter("url");
        try (PrintWriter out = resp.getWriter()) {
            if (url == null || url.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Missing 'url' parameter\"}");
                return;
            }

            url = url.trim();
            if (url.length() > MAX_URL_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"'url' parameter too long\"}");
                return;
            }

            boolean reachable;
            try {
                reachable = isHostReachable(url, DEFAULT_TIMEOUT_MS);
            } catch (IllegalArgumentException iae) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Invalid URL\"}");
                return;
            } catch (Exception e) {
                // don't leak internal details
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\":\"Internal server error\"}");
                return;
            }

            String json = String.format("{\"url\":\"%s\",\"reachable\":%b}",
                    escapeJson(url), reachable);
            resp.setStatus(HttpServletResponse.SC_OK);
            out.write(json);
        }
    }

    /**
     * Check whether a URL's host is reachable without invoking external commands.
     * - Accepts only http/https URLs.
     * - Validates host format.
     * - Tries InetAddress.isReachable() and TCP connect to common ports.
     *
     * Throws IllegalArgumentException on invalid input.
     */
    private static boolean isHostReachable(String urlString, int timeoutMs) throws IllegalArgumentException {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new IllegalArgumentException("Unsupported protocol");
            }

            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Missing host");
            }
            if (host.length() > 255 || !HOST_PATTERN.matcher(host).matches()) {
                throw new IllegalArgumentException("Invalid host");
            }

            InetAddress address = InetAddress.getByName(host);

            // Try ICMP/TCP (may require privileges for ICMP on some platforms)
            try {
                if (address.isReachable(timeoutMs)) {
                    return true;
                }
            } catch (IOException ignored) {
                // fall through to TCP connect attempts
            }

            // Try connecting to common service ports (HTTP/HTTPS)
            int[] portsToTry = {80, 443};
            for (int port : portsToTry) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(address, port), timeoutMs);
                    return true;
                } catch (IOException ignored) {
                    // try next port
                }
            }

            return false;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL", e);
        } catch (UnknownHostException e) {
            // Host not resolvable -> unreachable (not an input validation error)
            return false;
        }
    }

    // Minimal JSON string escaper (prevents injection of quotes/newlines in JSON)
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
// ...existing code...
