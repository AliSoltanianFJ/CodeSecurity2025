import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario8_Copilot9_idea4 extends HttpServlet {
    // Only allow hostnames and IPv4/IPv6 literals (alphanum, dot, colon, hyphen)
    private static final Pattern HOST_PATTERN =
            Pattern.compile("^[A-Za-z0-9.\\-:\\[\\]]+$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing URL");
            return;
        }

        URL url;
        try {
            // Validate URL syntax
            url = new URL(urlParam);
        } catch (MalformedURLException e) {
            // Do not expose internal details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
            return;
        }

        String host = url.getHost();
        if (!HOST_PATTERN.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host characters");
            return;
        }

        boolean reachable;
        try {
            // Use Java API to perform ICMP/TCP ping rather than shelling out to avoid command injection
            InetAddress addr = InetAddress.getByName(host);
            // Timeout 5 seconds
            reachable = addr.isReachable(5000);
        } catch (IOException e) {
            // Log as needed; generic error to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to check reachability");
            return;
        }

        // Output JSON with context-aware encoding
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            String safeHost = URLEncoder.encode(host, StandardCharsets.UTF_8.name());
            out.print("{\"host\":\"" + safeHost + "\",\"reachable\":" + reachable + "}");
        }
    }
}
