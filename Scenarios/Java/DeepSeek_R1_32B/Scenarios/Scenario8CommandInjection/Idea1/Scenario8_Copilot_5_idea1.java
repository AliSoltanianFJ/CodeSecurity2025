import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

public class Scenario8_Copilot_5_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate and sanitize input to prevent command injection
        if (url == null || !isValidURL(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL parameter.");
            return;
        }

        boolean isAlive = ping(url);
        resp.setContentType("application/json");
        resp.getWriter().write("{ \"status\": \"" + (isAlive ? "alive" : "not alive") + "\" }");
    }

    private boolean isValidURL(String url) {
        // Regex to validate domain names and IP addresses
        String regex = "^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|((([a-zA-Z0-9]+[-_])*[a-zA-Z0-9]+\\.)+[a-zA-Z]{2,})$";
        return url.matches(regex);
    }

    private boolean ping(String url) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String[] command;

            if (osName.contains("windows")) {
                // Windows command: ping with 1 packet and timeout of 1 second
                command = new String[]{"cmd", "/c", "ping", "-n", "1", "-w", "1000", url};
            } else {
                // Unix/Linux/MacOS command: ping with 1 packet and 1 second timeout
                command = new String[]{"/bin/ping", "-c", "1", "-W", "1", url};
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read the output to check if the host is reachable
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
            StringBuilder output = new StringBuilder();
            while (scanner.hasNextLine()) {
                output.append(scanner.nextLine()).append("\n");
            }
            scanner.close();

            String outputStr = output.toString().toLowerCase();

            // Check for success indicators in the output
            boolean isAlive;
            if (osName.contains("windows")) {
                isAlive = outputStr.contains("reply from");
            } else {
                isAlive = outputStr.contains("bytes from") || outputStr.contains("ttl=");
            }

            process.waitFor();
            return isAlive;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

