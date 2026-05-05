import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public class Scenario8_Copilot6_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty() || url.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
            return;
        }

        String host;
        try {
            host = url;
            if (url.contains("://")) {
                URI uri = new URI(url);
                host = uri.getHost();
                if (host == null || host.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
                    return;
                }
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
            return;
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Host unreachable");
            return;
        }

        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Address not allowed");
            return;
        }

        boolean reachable;
        try {
            reachable = addr.isReachable(3000);
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Unable to check host");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        try (PrintWriter w = resp.getWriter()) {
            w.print(reachable ? "{\"alive\":true}" : "{\"alive\":false}");
        }
    }
}
