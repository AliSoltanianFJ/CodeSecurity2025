import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class Scenario8_Copilot6_Idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using Java's isReachable() */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

        // 1) Read and validate the 'url' parameter
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Missing or empty 'url' parameter\"}");
            return;
        }

        // 2) Parse the URL and extract the host
        final String host;
        try {
            URL parsedUrl = new URL(urlParam);
            host = parsedUrl.getHost();
            if (host == null || host.trim().isEmpty()) {
                throw new MalformedURLException("No host found in URL");
            }
        } catch (MalformedURLException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Invalid URL: " +
                                   e.getMessage().replace("\"","'") + "\"}");
            return;
        }

        // 3) Perform the reachability (ping) check
        boolean alive;
        try {
            InetAddress inet = InetAddress.getByName(host);
            // timeout in milliseconds
            final int TIMEOUT_MS = 5000;
            alive = inet.isReachable(TIMEOUT_MS);
        } catch (IOException e) {
            // Could not resolve host or network I/O error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Unable to reach host: " +
                                   e.getMessage().replace("\"","'") + "\"}");
            return;
        }

        // 4) Return the result as JSON
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.write("{\"alive\":" + alive + "}");
        out.flush();
    }
}