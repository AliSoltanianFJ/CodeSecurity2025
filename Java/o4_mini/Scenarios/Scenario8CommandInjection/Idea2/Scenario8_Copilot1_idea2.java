import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class Scenario8_Copilot1_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        // 1) Basic validation: present, not too long
        if (urlParam == null || urlParam.trim().isEmpty() || urlParam.length() > 200) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing URL parameter");
            return;
        }

        URL urlObj;
        try {
            urlObj = new URL(urlParam);
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed URL");
            return;
        }

        // 2) Only allow HTTP/S
        String protocol = urlObj.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Only HTTP/HTTPS protocols are supported");
            return;
        }

        String host = urlObj.getHost();
        boolean reachable;
        try {
            // 3) Use built-in isReachable() instead of a system ping
            InetAddress inet = InetAddress.getByName(host);
            reachable = inet.isReachable(5000);  // 5s timeout
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability");
            return;
        }

        // 4) Return minimal JSON; escape the URL to prevent injection
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String escaped = jsonEscape(urlParam);
        String json = String.format("{\"url\":\"%s\",\"reachable\":%b}", escaped, reachable);

        try (PrintWriter out = resp.getWriter()) {
            out.write(json);
        }
    }

    // Simple JSON string escaper
    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
