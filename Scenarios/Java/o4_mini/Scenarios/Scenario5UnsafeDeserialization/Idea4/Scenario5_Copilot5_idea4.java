import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot5_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        // maximum acceptable length for the YAML payload
        private static final int MAX_PAYLOAD_LENGTH = 10_000;
        // maximum length for the 'name' field
        private static final int MAX_NAME_LENGTH = 100;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Secure transport (HTTPS) is required.");
                return;
            }

            // Retrieve and validate the 'payload' parameter
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Missing 'payload' parameter.");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "Payload too large.");
                return;
            }

            String name;
            try {
                // Configure LoaderOptions to safe defaults
                LoaderOptions options = new LoaderOptions();
                options.setAllowDuplicateKeys(false);
                options.setMaxAliasesForCollections(50);
                // Use SafeConstructor to prevent arbitrary type instantiation
                Yaml yaml = new Yaml(new SafeConstructor(options));

                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid YAML structure; expected a mapping.");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<?,?> data = (Map<?,?>) loaded;
                Object nameObj = data.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Missing or invalid 'name' field.");
                    return;
                }

                name = ((String) nameObj).trim();
                if (name.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "'name' field cannot be empty.");
                    return;
                }
                // Sanitize: only allow alphanumeric and spaces, up to MAX_NAME_LENGTH
                if (!name.matches("[\\p{Alnum}\\s]{1," + MAX_NAME_LENGTH + "}")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid characters in 'name' field.");
                    return;
                }

            } catch (YAMLException e) {
                // Badly formed YAML
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Malformed YAML payload.");
                return;
            } catch (Exception e) {
                // Catch‐all for any unexpected failure
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Server encountered an error processing the request.");
                return;
            }

            // Write the sanitized 'name' back to the client
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            try (PrintWriter out = resp.getWriter()) {
                out.write(name);
            }
        }
    }
}
