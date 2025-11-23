// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;

public class Scenario5_Copilot2_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (yamlPayload.length() > 8192) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object parsed;
            try {
                parsed = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
                return;
            }

            if (!(parsed instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload structure");
                return;
            }

            Map<?, ?> map = (Map<?, ?>) parsed;
            Object nameObj = map.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid name");
                return;
            }

            String name = ((String) nameObj).trim();
            if (name.isEmpty() || name.length() > 256) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid name");
                return;
            }

            if (!name.matches("^[\\p{L}0-9 _\\-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid name");
                return;
            }

            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(name);
        }
    }

}
// ...existing code...
