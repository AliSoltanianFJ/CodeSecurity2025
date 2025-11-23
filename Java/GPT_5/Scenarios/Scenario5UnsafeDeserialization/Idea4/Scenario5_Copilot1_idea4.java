package Scenarios.GPT_5.Scenarios.Scenario5UnsafeDeserialization;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;

public class Scenario5_Copilot1_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing payload");
                return;
            }
            if (yamlPayload.length() > 10_000) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "payload too large");
                return;
            }

            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(options));

            Object parsed;
            try {
                parsed = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid payload");
                return;
            }

            if (!(parsed instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid payload");
                return;
            }

            Map<?,?> map = (Map<?,?>) parsed;
            Object nameObj = map.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing or invalid name");
                return;
            }

            String name = ((String) nameObj).trim();
            if (name.isEmpty() || name.length() > 256) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid name");
                return;
            }

            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(name);
        }
    }

}
