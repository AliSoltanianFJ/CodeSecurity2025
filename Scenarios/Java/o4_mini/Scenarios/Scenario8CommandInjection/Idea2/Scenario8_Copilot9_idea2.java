import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario8_Copilot9_idea2 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        String host = urlParam.trim();
        // Validate hostname or IPv4 address (letters, digits, dots, hyphens)
        if (!host.matches("^[a-zA-Z0-9.-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        boolean reachable;
        try {
            InetAddress inet = InetAddress.getByName(host);
            // timeout 5000ms
            reachable = inet.isReachable(5000);
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Host not found");
            return;
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability");
            return;
        }

        resp.setContentType("application/json; charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.print("{\"host\":\"" + escapeJson(host) + "\",\"reachable\":" + reachable + "}");
        }
    }

    /**
     * Escapes JSON special characters in a string.
     */
    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '/':  sb.append("\\/");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
