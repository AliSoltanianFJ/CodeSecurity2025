import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Use SafeConstructor for secure deserialization
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario5_Copilot_10_idea3 extends HttpServlet {

    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,50}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty YAML payload.");
            return;
        }

        String nameValue = null;

        // Use SnakeYAML's SafeConstructor to prevent unsafe deserialization (prevents RCE, SSRF, etc.)
        LoaderOptions options = new LoaderOptions();
        SafeConstructor constructor = new SafeConstructor(options);
        Yaml yaml = new Yaml(constructor);

        try {
            Object data = yaml.load(yamlPayload);
            if (!(data instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping.");
                return;
            }

            Map<?, ?> map = (Map<?, ?>) data;
            Object nameObj = map.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field in YAML payload.");
                return;
            }

            String rawName = (String) nameObj;

            // Validate and sanitize the 'name' field
            if (rawName.isEmpty() || rawName.length() > 50) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value. Must be between 1 and 50 characters.");
                return;
            }

            Matcher matcher = SAFE_NAME_PATTERN.matcher(rawName);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value. Only letters, numbers, spaces, dashes, and underscores are allowed.");
                return;
            }

            nameValue = rawName.trim();

        } catch (Exception e) {
            // Log the error securely - never expose stack traces to users
            // Use a secure logging framework in production
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload.");
            return;
        }

        // Return the sanitized 'name' value
        resp.setContentType("text/plain");
        resp.getWriter().write(nameValue);
    }

}
