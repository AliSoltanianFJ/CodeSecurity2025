import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot8_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        private static final int MAX_PAYLOAD_SIZE = 10_000;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate input
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter.");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large.");
                return;
            }

            // Deserialize safely using SafeConstructor
            Map<String, Object> data;
            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                data = yaml.load(yamlPayload);
            } catch (YAMLException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload.");
                return;
            }

            if (data == null || !data.containsKey("name")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field in payload.");
                return;
            }
            Object nameObj = data.get("name");
            String name = nameObj == null ? "" : nameObj.toString();

            // Return as plain text to avoid XSS
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(name);
        }
    }
}
