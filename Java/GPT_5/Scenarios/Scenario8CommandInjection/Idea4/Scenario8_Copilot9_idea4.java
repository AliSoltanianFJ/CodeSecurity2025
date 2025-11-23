import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Scenario8_Copilot9_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isEmpty() || urlParam.length() > 2083) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid url");
            return;
        }

        URL url;
        try {
            url = new URL(urlParam);
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid url");
            return;
        }

        String scheme = url.getProtocol();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid url");
            return;
        }

        String host = url.getHost();
        if (host == null || host.isEmpty() || host.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid url");
            return;
        }

        boolean reachable;
        try {
            reachable = pingUrl(url);
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "target unreachable");
            return;
        }

        if (reachable) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain;charset=utf-8");
            resp.getWriter().write("alive");
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "target unreachable");
        }
    }

    private boolean pingUrl(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } finally {
            conn.disconnect();
        }
    }
}
