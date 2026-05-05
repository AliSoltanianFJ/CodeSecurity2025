import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario8_Copilot8_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario8.class.getName());
    private static final Pattern HOST_PATTERN = Pattern.compile(
        "^(?=.{1,253}$)(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$"
        + "|^((25[0-5]|2[0-4]\\\\d|[01]?\\\\d\\\\d?)(\\.(25[0-5]|2[0-4]\\\\d|[01]?\\\\d\\\\d?)){3})$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String host = req.getParameter("url");
        if (host == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing parameter");
            return;
        }
        host = host.trim();
        if (host.isEmpty() || host.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid parameter");
            return;
        }
        if (!HOST_PATTERN.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid host");
            return;
        }

        boolean ok = pingHost(host);
        if (ok) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("reachable");
        } else {
            resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, "unreachable");
        }
    }

    private boolean pingHost(String host) {
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", host);
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Ping operation failed", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (SecurityException e) {
            logger.log(Level.WARNING, "Insufficient permission to execute ping", e);
            return false;
        }
    }
}
