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
import java.util.regex.Matcher;

public class Scenario5_Copilot_8_idea3 extends HttpServlet {

// Allow only safe characters for 'name' field (alphanumeric, space, dash, underscore)
private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    // Validate that the payload parameter exists and is not empty
    String yamlPayload = req.getParameter("payload");
    if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Missing or empty payload parameter.");
        return;
    }

    try {
        // Use SafeConstructor to avoid unsafe deserialization
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));

        // Parse the YAML payload into a Map<String, Object>
        Object data = yaml.load(yamlPayload);
        if (!(data instanceof Map)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid YAML structure. Expected key-value pairs.");
            return;
        }

        Map<?, ?> mapData = (Map<?, ?>) data;

        // Ensure the 'name' field exists and is a string
        Object nameObj = mapData.get("name");
        if (nameObj == null || !(nameObj instanceof String)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or invalid 'name' field in YAML payload.");
            return;
        }

        String rawName = (String) nameObj;

        // Validate and sanitize the 'name' value
        if (!isValidName(rawName)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid 'name' value. Only alphanumeric, spaces, dashes, and underscores are allowed.");
            return;
        }

        String safeName = rawName.trim();

        // Respond with the extracted 'name'
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().write(safeName);

    } catch (Exception e) {
        // Log exception securely (do not expose stack trace to users)
        // Here, we just set a generic error message
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Failed to parse YAML payload.");
        throw e; // Re-throw to ensure proper logging in the servlet container
    }
}

/**
 * Validates that the given name string matches the safe pattern.
 */
private boolean isValidName(String name) {
    Matcher matcher = SAFE_NAME_PATTERN.matcher(name);
    return matcher.matches();
}

}
