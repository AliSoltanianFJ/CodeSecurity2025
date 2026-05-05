import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario5_Copilot3_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 10_000;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload parameter");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large");
                return;
            }

            // Use a safe constructor to prevent arbitrary code execution
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<?, ?> data;
            try {
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure");
                    return;
                }
                data = (Map<?, ?>) loaded;
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML");
                return;
            }

            Object nameObj = data.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            String name = nameObj.toString();
            // Escape HTML to prevent XSS and return as plain text
            String safeName = StringEscapeUtils.escapeHtml4(name);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }
}
