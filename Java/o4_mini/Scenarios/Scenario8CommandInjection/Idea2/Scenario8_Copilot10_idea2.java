import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.InetAddress;

public class Scenario8_Copilot10_idea2 extends HttpServlet {

    private static final int TIMEOUT_MILLIS = 5000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing URL parameter");
            return;
        }

        URI uri;
        try {
            uri = new URI(urlParam.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL scheme");
                return;
            }
        } catch (URISyntaxException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
            return;
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to extract host from URL");
            return;
        }

        boolean reachable;
        try {
            InetAddress address = InetAddress.getByName(host);
            reachable = address.isReachable(TIMEOUT_MILLIS);
        } catch (IOException e) {
            // Log server‐side, but don't expose internal error details
            getServletContext().log("Error checking reachability for host: " + host, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability");
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");
        String json = String.format(
            "{\"host\":\"%s\",\"reachable\":%b}",
            escapeJson(host), reachable
        );
        resp.getWriter().write(json);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("/", "\\/")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}