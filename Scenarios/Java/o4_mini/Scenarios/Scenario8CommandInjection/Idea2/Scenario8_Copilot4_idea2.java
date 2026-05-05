import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario8_Copilot4_idea2 extends HttpServlet {
    private static final Pattern HOST_PATTERN =
        Pattern.compile("^(?=.{1,253}$)(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.?$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }
        String host = urlParam.trim();
        if (host.length() > 253 || !HOST_PATTERN.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host provided");
            return;
        }

        boolean reachable;
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            reachable = inetAddress.isReachable(5000); // 5-second timeout
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown host");
            return;
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html><head><meta charset='UTF-8'><title>Ping Result</title></head><body>");
            String escapedHost = StringEscapeUtils.escapeHtml4(host);
            String status = reachable ? "Host is reachable" : "Host is not reachable";
            writer.println("<p>" + escapedHost + ": " + StringEscapeUtils.escapeHtml4(status) + "</p>");
            writer.println("</body></html>");
        }
    }
}
