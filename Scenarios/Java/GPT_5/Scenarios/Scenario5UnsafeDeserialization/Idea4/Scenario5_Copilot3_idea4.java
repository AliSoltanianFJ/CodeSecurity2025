// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.YAMLException;
import java.util.Map;

public class Scenario5_Copilot3_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (yamlPayload.length() > 10_000) {
                resp.sendError(413, "Payload too large");
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Object parsed = yaml.load(yamlPayload);
                if (!(parsed instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload structure");
                    return;
                }
                Map<?,?> map = (Map<?,?>) parsed;
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
                for (int i = 0; i < name.length(); i++) {
                    if (Character.isISOControl(name.charAt(i))) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid characters in name");
                        return;
                    }
                }

                resp.setContentType("text/plain;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(name);
            } catch (YAMLException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
// ...existing code...
