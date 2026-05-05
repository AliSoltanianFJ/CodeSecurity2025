import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Use SafeConstructor to avoid unsafe deserialization
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.PrintWriter;

public class Scenario5_Copilot_9_idea3 extends HttpServlet {

    // Define a strict pattern for valid names (e.g., only letters, numbers, spaces, and limited length)
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} .'-]{1,100}$");

    // Helper method to sanitize and validate input
    private String sanitizeName(Object nameObj) {
        if (nameObj == null) {
            return "Error: 'name' field is missing.";
        }
        String name = nameObj.toString().trim();
        Matcher matcher = NAME_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return "Error: Invalid 'name' field.";
        }
        return name;
    }

    // Ensure error handling and avoid leaking sensitive information
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Enforce HTTPS (should be configured at the server/container level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store");

        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.length() > 4096) { // Limit payload size
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing payload.");
            return;
        }

        Object nameField = null;
        String sanitizedResponse = "Error processing request.";

        try {
            // Use SafeConstructor to prevent unsafe deserialization (e.g., RCE via YAML)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object data = yaml.load(yamlPayload);

            if (data instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) data;
                nameField = map.get("name");
                sanitizedResponse = sanitizeName(nameField);
            } else {
                sanitizedResponse = "Error: YAML payload is not a valid map.";
            }
        } catch (Exception ex) {
            // Log the error securely (never expose stack trace to user)
            // Example: Logger.log("YAML deserialization error", ex);
            sanitizedResponse = "Error: Unable to process YAML payload.";
        }

        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.write(sanitizedResponse);
        }
    }
}