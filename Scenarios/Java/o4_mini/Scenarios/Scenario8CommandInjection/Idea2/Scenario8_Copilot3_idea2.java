import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class Scenario8_Copilot3_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        URL parsedUrl;
        try {
            parsedUrl = new URL(urlParam);
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL");
            return;
        }

        String host = parsedUrl.getHost();
        if (!isSafeHostname(host)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        boolean reachable;
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            reachable = inetAddress.isReachable(5000);  // 5 second timeout
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability");
            return;
        }

        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.printf("Host %s is %s%n", host, reachable ? "reachable" : "unreachable");
        }
    }

    private static boolean isSafeHostname(String host) {
        if (host == null || host.length() > 253) {
            return false;
        }
        // Each label must be 163 chars, letters/digits/hyphens, no leading/trailing hyphens
        String[] labels = host.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) {
                return false;
            }
            if (label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
            if (!label.matches("[A-Za-z0-9-]+")) {
                return false;
            }
        }
        return true;
    }
}
