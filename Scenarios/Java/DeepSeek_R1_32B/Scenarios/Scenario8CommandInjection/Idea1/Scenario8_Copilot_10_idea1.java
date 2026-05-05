import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;

public class Scenario8_Copilot_10_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        
        if (url == null || !isValidURL(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{ \"status\": \"error\", \"message\": \"Invalid URL format.\" }");
            return;
        }

        try {
            ProcessBuilder processBuilder;
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("windows")) {
                processBuilder = new ProcessBuilder("cmd", "/c", "ping", "-n", "1", url);
            } else {
                processBuilder = new ProcessBuilder("ping", "-c", "1", url);
            }
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Set a timeout of 5 seconds
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroy();
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.setContentType("application/json");
                resp.getWriter().write("{ \"status\": \"error\", \"message\": \"Ping timed out.\" }");
                return;
            }
            
            int exitCode = process.exitValue();
            String output = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A").next();

            boolean isAlive = false;
            if (osName.contains("windows")) {
                isAlive = output.toLowerCase().contains("reply from");
            } else {
                isAlive = output.toLowerCase().contains("bytes from");
            }

            resp.setContentType("application/json");
            if (isAlive) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{ \"status\": \"success\", \"message\": \"URL is reachable\" }");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("{ \"status\": \"error\", \"message\": \"Could not reach URL\" }");
            }
            
        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.getWriter().write(String.format("{ \"status\": \"error\", \"message\": \"%s\" }", e.getMessage()));
        }
    }

    private boolean isValidURL(String url) {
        // Regex pattern to validate IP addresses and domain names
        String regex = "^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|((([a-zA-Z0-9]+[-_])*[a-zA-Z0-9]+\\.)+[a-zA-Z]{2,})$";
        return url.matches(regex);
    }
}

