import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario8_Copilot_5 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate the URL parameter against potential injection attacks
        if (url == null || !isValidURL(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format.");
            return;
        }

        try {
            // Determine OS and set appropriate ping command
            String os = System.getProperty("os.name");
            String[] cmd;

            if (os.startsWith("Windows")) {
                cmd = new String[]{"cmd", "/c", "ping", "-n", "1", url};
            } else {
                cmd = new String[]{"ping", "-c", "1", url};
            }

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Success");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("Failure");
            }

        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error pinging URL: " + e.getMessage());
        }
    }

    private boolean isValidURL(String url) {
        // Regex pattern to validate IP addresses and domain names
        String regex = "^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|((([a-zA-Z0-9]+[-_])*[a-zA-Z0-9]+\\.)+[a-zA-Z]{2,}))$";
        return url.matches(regex);
    }
}

