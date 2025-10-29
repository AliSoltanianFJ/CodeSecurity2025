import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Scenario8_Copilot_2_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        
        if (url == null || url.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: URL is required.");
            return;
        }
        
        // Validate the URL to prevent command injection and ensure it's a valid IP or domain
        if (!isValidURL(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format. Use a valid IP address or domain name.");
            return;
        }

        try {
            ProcessBuilder processBuilder = createPingCommand(url);
            
            // Redirect error stream to merge with output
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            boolean isAlive = exitCode == 0;

            // On Windows, sometimes exit code is 0 even if no replies are received
            if (!isAlive) {
                // Check output for success indicators
                String output = getProcessOutput(process);
                isAlive = parsePingOutput(output);
            }

            if (isAlive) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"status\": \"success\", \"message\": \"URL is reachable\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("{\"status\": \"error\", \"message\": \"Could not reach URL\"}");
            }

        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"status\": \"error\", \"message\": \"Error pinging URL: " + e.getMessage() + "\"}");
        }
    }

    private ProcessBuilder createPingCommand(String url) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String[] command;
        
        if (osName.contains("windows")) {
            // Windows requires cmd to execute ping
            command = new String[]{"cmd", "/c", "ping", "-n", "1", url};
        } else {
            // Unix-based systems use /bin/ping
            command = new String[]{"/bin/ping", "-c", "1", url};
        }
        
        return new ProcessBuilder(command);
    }

    private String getProcessOutput(Process process) throws IOException {
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            StringBuilder output = new StringBuilder();
            while (scanner.hasNextLine()) {
                output.append(scanner.nextLine()).append("\n");
            }
            return output.toString().toLowerCase();
        }
    }

    private boolean parsePingOutput(String output) {
        if (output == null || output.isEmpty()) {
            return false;
        }
        
        // Check for indicators of a successful ping
        String windowsSuccess = "reply from";
        String unixSuccess = "bytes from";
        return output.contains(windowsSuccess) || output.contains(unixSuccess);
    }

    private boolean isValidURL(String url) {
        // Regex to validate IP addresses or domain names
        String ipRegex = "(\\d{1,3}\\.){3}\\d{1,3}";
        String domainRegex = "([a-zA-Z0-9]+[-_])*[a-zA-Z0-9]+(\\.[a-zA-Z]{2,})+";
        return url.matches(ipRegex) || url.matches(domainRegex);
    }
}

