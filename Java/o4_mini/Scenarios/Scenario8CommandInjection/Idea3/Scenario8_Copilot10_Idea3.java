import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot10_Idea3 extends HttpServlet {
    /** 
     * Handles POST /ping with form-param "url". 
     * Replies with JSON {"host":"…","reachable":true|false} 
     * or {"error":"…"} on bad input or failure.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) read and basic‐trim
        String urlParam = req.getParameter("url");
        resp.setContentType("application/json");
        try (PrintWriter out = resp.getWriter()) {
            // 2) missing?
            if (urlParam == null || urlParam.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Missing 'url' parameter\"}");
                return;
            }

            // 3) extract host (if full URL) or take as is
            String host;
            try {
                URL u = new URL(urlParam.trim());
                host = u.getHost();
                // if URL was e.g. "http:///" and getHost() is empty, fallback:
                if (host == null || host.isEmpty()) {
                    host = urlParam.trim();
                }
            } catch (MalformedURLException e) {
                host = urlParam.trim();
            }

            // 4) validate host: only letters, digits, dots and hyphens
            //    (rejects any attempt to inject spaces, semicolons, &, |, etc.)
            if (!host.matches("^[A-Za-z0-9.-]+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Invalid host format\"}");
                return;
            }

            // 5) ping it
            boolean reachable;
            try {
                reachable = pingHost(host);
            } catch (InterruptedException e) {
                // restore interrupt status
                Thread.currentThread().interrupt();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\":\"Ping was interrupted\"}");
                return;
            }

            // 6) success
            resp.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"host\":\"" + host + "\",\"reachable\":" + reachable + "}");
        }
    }

    /**
     * Runs the native 'ping' command in a secure way (no shell),
     * waits up to 10 seconds, and returns true if exit code == 0.
     */
    private boolean pingHost(String host) throws IOException, InterruptedException {
        // build command list
        List<String> cmd = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: ping -n 4 host
            cmd.add("ping");
            cmd.add("-n");
            cmd.add("4");
            cmd.add(host);
        } else {
            // Unix‐like: ping -c 4 host
            cmd.add("ping");
            cmd.add("-c");
            cmd.add("4");
            cmd.add(host);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // merge stderr into stdout in case you ever want to log it
        pb.redirectErrorStream(true);

        Process p = pb.start();
        // wait up to 10 seconds
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return false;
        }
        return p.exitValue() == 0;
    }
}