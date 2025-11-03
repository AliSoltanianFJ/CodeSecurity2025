import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;

public class Scenario5_Copilot_5_idea3 extends HttpServlet {

    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1,100}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Validate input: payload parameter must not be null or empty
        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty payload parameter.");
            return;
        }

        try {
            // Use SafeConstructor to avoid unsafe deserialization (prevents RCE, SSRF, etc.)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object data = yaml.load(yamlPayload);

            // Ensure the deserialized object is a Map
            if (!(data instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be an object/map.");
                return;
            }

            Map<?, ?> mapData = (Map<?, ?>) data;

            // Extract 'name' field and validate it
            Object nameObj = mapData.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                return;
            }

            String nameValue = (String) nameObj;

            // Validate the 'name' value against a safe pattern
            if (!SAFE_NAME_PATTERN.matcher(nameValue).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value. Only alphanumeric, underscore, and hyphen are allowed.");
                return;
            }

            // Set proper content type and respond with the sanitized name
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(nameValue);

        } catch (Exception e) {
            // Log exception securely: do not expose details to client
            // e.g., use a secure logging framework, not shown here for brevity
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error processing the request.");
        }
    }

}
