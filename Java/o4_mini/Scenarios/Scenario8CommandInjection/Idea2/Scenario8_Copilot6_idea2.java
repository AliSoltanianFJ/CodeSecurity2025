import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario8_Copilot6_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // only letters, digits, hyphens and dots, max length 253
    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9\\-\\.]{1,253}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String host = req.getParameter("url");
        if (host == null || host.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing url parameter");
            return;
        }

        // Validate overall form
        if (!HOST_PATTERN.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid hostname or IP");
            return;
        }

        // Validate each label does not exceed 63 characters
        for (String label : host.split("\\.")) {
            if (label.length() > 63) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid hostname or IP");
                return;
            }
        }

        boolean reachable = ping(host, 5000);

        // Return JSON response, escaping the host string
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String safeHost = escapeJson(host);
        resp.getWriter().write("{\"host\":\"" + safeHost + "\",\"reachable\":" + reachable + "}");
    }

    /**
     * Pings the given host within the specified timeout.
     * Uses Java's ICMP echo through InetAddress.isReachable(),
     * avoiding any system call injection.
     */
    private static boolean ping(String host, int timeoutMillis) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.isReachable(timeoutMillis);
        } catch (IOException e) {
            // on error, treat as unreachable
            return false;
        }
    }

    /**
     * Simple JSON string escaper: handles quotes and backslashes.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
