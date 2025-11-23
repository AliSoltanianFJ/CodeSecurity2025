// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class Scenario8_Copilot7_idea4 extends HttpServlet {
    private static final Pattern HOSTNAME = Pattern.compile("^(?=.{1,255}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(?:\\.(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?))*$");
    private static final Pattern IPV4 = Pattern.compile("^(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter");
            return;
        }

        String trimmed = url.trim();
        if (trimmed.isEmpty() || trimmed.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
            return;
        }

        String host = trimmed;
        if (trimmed.contains("://")) {
            try {
                URI uri = new URI(trimmed);
                host = uri.getHost();
                if (host == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
                    return;
                }
            } catch (URISyntaxException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
                return;
            }
        }

        if (host.contains("/") || host.contains("\\") || host.contains("%") || host.contains("\n") || host.contains("\r")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
            return;
        }

        if (!(HOSTNAME.matcher(host).matches() || IPV4.matcher(host).matches())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
            return;
        }

        try {
            InetAddress inet = InetAddress.getByName(host);
            boolean reachable = inet.isReachable(3000);
            resp.setContentType("application/json;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"reachable\":" + reachable + "}");
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Unable to reach host");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
        }
    }
}
// ...existing code...
