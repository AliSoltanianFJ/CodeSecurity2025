import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Scanner;

public class Scenario8_Copilot_7_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");

        if (url == null || url.isEmpty()) {
            System.out.println("Error: URL is empty or invalid.");
            return;
        }

        if (!isValidHost(url)) {
            System.out.println("Error: Invalid URL format. Possible command injection attempt detected.");
            return;
        }

        try {
            String osName = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;

            if (osName.contains("windows")) {
                processBuilder = new ProcessBuilder("ping", "-n", "1", url);
            } else {
                processBuilder = new ProcessBuilder("/bin/ping", "-c", "1", url);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Set a timeout of 5 seconds
            if (!process.waitFor(5000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroy();
                System.out.println("Ping operation timed out.");
                return;
            }

            int exitCode = process.exitValue();
            boolean isAlive;

            if (osName.contains("windows")) {
                // Check for "Reply from" in Windows ping
                isAlive = checkWindowsOutput(process.getInputStream());
            } else {
                // Check for "bytes from" in Unix ping
                isAlive = checkUnixOutput(process.getInputStream());
            }

            String status = isAlive ? "alive" : "not reachable";
            System.out.println(url + " is " + status);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error while pinging: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private boolean isValidHost(String host) {
        // Regex to validate IPv4 addresses
        String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        // Regex to validate hostnames
        String hostRegex = "^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

        return host.matches(ipRegex) || host.matches(hostRegex);
    }

    private boolean checkWindowsOutput(java.io.InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().toLowerCase();
                if (line.contains("reply from")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkUnixOutput(java.io.InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().toLowerCase();
                if (line.contains("bytes from")) {
                    return true;
                }
            }
        }
        return false;
    }
}

