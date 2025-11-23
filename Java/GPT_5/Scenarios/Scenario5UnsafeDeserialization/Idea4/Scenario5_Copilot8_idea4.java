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

public class Scenario5_Copilot8_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (yamlPayload.length() > 4096) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large");
                return;
            }
            LoaderOptions options = new LoaderOptions();
            options.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Object parsed;
            try {
                parsed = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
                return;
            }
            if (!(parsed instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected mapping payload");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) parsed;
            Object nameObj = map.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field");
                return;
            }
            String name = ((String) nameObj).trim();
            if (name.isEmpty() || name.length() > 200) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value");
                return;
            }
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(name);
        }
    }

}
