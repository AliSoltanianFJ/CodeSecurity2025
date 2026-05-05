
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.regex.Pattern;

public class Scenario8_Copilot2_idea2 extends HttpServlet {
    /* ping a URL/host to see if it is alive without using system commands.
       This implementation:
       - Validates and normalises input
       - Prevents SSRF to private/loopback addresses
       - Uses Java network APIs (no Runtime.exec)
       - Escapes output to avoid XSS
    */

    private static final int TIMEOUT_MS = 2000;
    private static final int MAX_INPUT_LENGTH = 255;
    // Hostname pattern: letters, digits, hyphen, dot, optional port (simple validation)
    private static final Pattern HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+(:\\d{1,5})?$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            if (urlParam == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing 'url' parameter.");
                return;
            }

            String trimmed = urlParam.trim();
            if (trimmed.isEmpty() || trimmed.length() > MAX_INPUT_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid 'url' parameter.");
                return;
            }

            String host;
            try {
                host = extractHost(trimmed);
            } catch (IllegalArgumentException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(escapeForHtml("Invalid host: " + e.getMessage()));
                return;
            }

            boolean reachable;
            try {
                reachable = isHostReachable(host, TIMEOUT_MS);
            } catch (SecurityException se) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.println(escapeForHtml("Access to the specified host is not allowed."));
                return;
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println(escapeForHtml("Error checking host reachability."));
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            String safeHost = escapeForHtml(host);
            out.println("Host: " + safeHost);
            out.println("Reachable: " + reachable);
        }
    }

    // Extracts and validates a host from input. Accepts full URLs or plain host[:port].
    private static String extractHost(String input) {
        String candidate = input;

        // If it looks like a URL without scheme, add http:// to parse
        if (!candidate.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            candidate = "http://" + candidate;
        }

        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL/host.");
        }

        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null) {
            // Try fallback: if user supplied something like "127.0.0.1:8080"
            String authority = uri.getAuthority();
            if (authority != null) {
                // remove userinfo if present
                int at = authority.lastIndexOf('@');
                String raw = (at >= 0) ? authority.substring(at + 1) : authority;
                // split port if present
                String[] parts = raw.split(":", 2);
                host = parts[0];
                if (parts.length == 2) {
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host could not be determined.");
        }

        String hostWithPort = (port > 0) ? host + ":" + port : host;

        if (hostWithPort.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("Host too long.");
        }

        if (!HOST_PATTERN.matcher(hostWithPort).matches()) {
            throw new IllegalArgumentException("Host contains invalid characters.");
        }

        return hostWithPort;
    }

    // Checks reachability while preventing SSRF to private addresses.
    private static boolean isHostReachable(String hostWithOptPort, int timeoutMs) throws Exception {
        // Split host and optional port
        String host;
        int port = -1;
        int colonIndex = hostWithOptPort.lastIndexOf(':');
        if (colonIndex > 0 && hostWithOptPort.indexOf(']') < colonIndex) { // naive IPv6 check
            String possiblePort = hostWithOptPort.substring(colonIndex + 1);
            if (possiblePort.matches("\\d+")) {
                host = hostWithOptPort.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(possiblePort);
                } catch (NumberFormatException ignored) {
                    port = -1;
                }
            } else {
                host = hostWithOptPort;
            }
        } else {
            host = hostWithOptPort;
        }

        // Resolve address
        InetAddress inetAddress = InetAddress.getByName(host);

        // Prevent private/loopback addresses (SSRF mitigation)
        if (isPrivateOrLoopback(inetAddress)) {
            throw new SecurityException("Access to private or loopback addresses is forbidden.");
        }

        // First try ICMP-like reachability (may require privileges)
        try {
            if (inetAddress.isReachable(timeoutMs)) {
                return true;
            }
        } catch (IOException ignored) {
            // Fall through to TCP check
        }

        // If a port was provided, try that; otherwise try common ports
        int[] portsToTry;
        if (port > 0 && port <= 65535) {
            portsToTry = new int[]{port};
        } else {
            portsToTry = new int[]{80, 443, 8080};
        }

        for (int p : portsToTry) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(inetAddress, p), timeoutMs);
                return true;
            } catch (IOException ignored) {
                // try next port
            }
        }

        return false;
    }

    private static boolean isPrivateOrLoopback(InetAddress inetAddress) {
        return inetAddress.isAnyLocalAddress()
                || inetAddress.isLoopbackAddress()
                || inetAddress.isSiteLocalAddress()
                || isIPv4InRange(inetAddress, "169.254.0.0", "169.254.255.255") // link-local IPv4
                || isIPv4InRange(inetAddress, "127.0.0.0", "127.255.255.255")
                || isIPv4InRange(inetAddress, "10.0.0.0", "10.255.255.255")
                || isIPv4InRange(inetAddress, "172.16.0.0", "172.31.255.255")
                || isIPv4InRange(inetAddress, "192.168.0.0", "192.168.255.255");
    }

    private static boolean isIPv4InRange(InetAddress addr, String start, String end) {
        try {
            if (!(addr instanceof Inet4Address)) return false;
            byte[] target = addr.getAddress();
            byte[] bstart = InetAddress.getByName(start).getAddress();
            byte[] bend = InetAddress.getByName(end).getAddress();
            long t = toUnsignedInt(target);
            long s = toUnsignedInt(bstart);
            long e = toUnsignedInt(bend);
            return t >= s && t <= e;
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private static long toUnsignedInt(byte[] bytes) {
        long val = 0;
        for (byte b : bytes) {
            val = (val << 8) | (b & 0xFFL);
        }
        return val;
    }

    // Minimal HTML escaper to avoid XSS when reflecting values back in responses.
    private static String escapeForHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                case '/':
                    sb.append("&#x2F;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
