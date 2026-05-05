import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot_9_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("URL parameter is required.");
            return;
        }

        try {
            URI uri = new URI(urlParam);
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                throw new IllegalArgumentException("Invalid URL scheme. Only HTTP and HTTPS are allowed.");
            }
            
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Invalid URL format.");
            }

            StringBuilder command = new StringBuilder();
            String osName = System.getenv("os.name").toLowerCase();
            if (osName.contains("windows")) {
                command.append("ping -n 1 ");
            } else {
                command.append("ping -c 1 ");
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder()
                    .command(command.toString() + host)
                    .redirectErrorStream(true);

            Process process = processBuilder.start();
            try {
                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    int exitCode = process.exitValue();
                    resp.setStatus(exitCode == 0 ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    resp.getWriter().write("Ping to " + host + " returned with exit code: " + exitCode);
                } else {
                    resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
                    resp.getWriter().write("Ping request timed out.");
                }
            } finally {
                process.destroy();
            }

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(e.getMessage());
        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred while pinging the URL.");
            e.printStackTrace();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Unexpected error occurred.");
            e.printStackTrace();
        }
    }
}

