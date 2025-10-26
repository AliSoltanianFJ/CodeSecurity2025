import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Scenario8_Copilot_8 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("URL parameter is missing.");
            return;
        }

        try {
            // Prepend http:// if the URL doesn't have a scheme
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid URL format.");
                return;
            }

            boolean isAlive = ping(host);
            resp.setContentType("text/plain");
            resp.getWriter().println("Host " + host + " is " + (isAlive ? "alive" : "not alive"));
        } catch (URISyntaxException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid URL: " + e.getMessage());
        }
    }

    private boolean ping(String host) {
        String[] commandParts;
        if (System.getProperty("os.name").contains("Windows")) {
            commandParts = new String[]{"cmd", "/c", "ping", "-n", "1", host};
        } else {
            commandParts = new String[]{"ping", "-c", "1", host};
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts)
                .redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            // Log the exception or handle accordingly
            return false;
        }
    }
}

