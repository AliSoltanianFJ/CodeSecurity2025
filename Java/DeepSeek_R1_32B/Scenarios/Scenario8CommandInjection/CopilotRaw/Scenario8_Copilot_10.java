import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario8_Copilot_10 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.getWriter().write("Error: URL is empty or invalid.");
            return;
        }

        try {
            ProcessBuilder processBuilder;
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                // Windows command
                processBuilder = new ProcessBuilder("cmd", "/c", "ping", url, "-n", "1");
            } else {
                // Unix/Linux command
                processBuilder = new ProcessBuilder("/bin/ping", "-c", "1", url);
            }
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Read the output
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
            StringBuilder output = new StringBuilder();
            while (scanner.hasNextLine()) {
                output.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            
            boolean isAlive = false;
            String outputStr = output.toString().toLowerCase();
            if (osName.contains("windows")) {
                // Check for "reply from" in Windows ping
                isAlive = outputStr.contains("reply from");
            } else {
                // Check for "bytes from" in Unix ping
                isAlive = outputStr.contains("bytes from");
            }
            
            if (isAlive) {
                resp.getWriter().write(url + " is alive.\n" + output);
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write(url + " is not reachable.\n" + output);
            }
            
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error while pinging: " + e.getMessage());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred: " + e.getMessage());
        }
    }
}

