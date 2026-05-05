import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.IDN;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

public class Scenario8_Copilot9_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive without invoking an OS command (avoids command injection).
       Uses InetAddress.isReachable and falls back to a TCP connect test if necessary. */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        String host;
        try {
            // Accept either full URL (http://...) or plain hostname/IP
            if (url.contains("://")) {
                URL parsed = new URL(url);
                host = parsed.getHost();
                if (host == null || host.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL does not contain a host");
                    return;
                }
            } else {
                host = url.trim();
            }

            String result = ping(host);

            resp.setContentType("text/plain; charset=utf-8");
            try (PrintWriter w = resp.getWriter()) {
                w.write(result);
            }
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            // IO problems producing response or during network tests
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error: " + e.getMessage());
        } catch (Exception e) {
            // Catch-all so servlet always responds with a proper HTTP error code on unexpected failures
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    private String ping(String target) throws IOException {
        if (target == null || target.trim().isEmpty()) {
            return "Invalid target";
        }

        // Normalize internationalized hostnames to ASCII form
        final String asciiHost;
        try {
            asciiHost = IDN.toASCII(target.trim());
        } catch (Exception e) {
            return "Invalid host name: " + target;
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName(asciiHost);
        } catch (UnknownHostException e) {
            return "Unknown host: " + asciiHost;
        }

        // Try ICMP-based reachability (may require privileges / depend on JVM/OS)
        final int icmpTimeoutMs = 3000;
        boolean reachable;
        try {
            reachable = addr.isReachable(icmpTimeoutMs);
        } catch (IOException | SecurityException e) {
            // If isReachable fails (common on some platforms), fall back to TCP connect checks
            reachable = false;
        }

        // If ICMP check failed or returned false, try TCP connect on common ports (80, 443)
        if (!reachable) {
            final int tcpTimeoutMs = 2000;
            if (tryTcpConnect(addr, 80, tcpTimeoutMs) || tryTcpConnect(addr, 443, tcpTimeoutMs)) {
                reachable = true;
            }
        }

        String hostAddress = addr.getHostAddress();
        return reachable ? ("Reachable: " + asciiHost + " (" + hostAddress + ")")
                         : ("Unreachable: " + asciiHost + " (" + hostAddress + ")");
    }

    private boolean tryTcpConnect(InetAddress addr, int port, int timeoutMs) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(addr, port), timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
