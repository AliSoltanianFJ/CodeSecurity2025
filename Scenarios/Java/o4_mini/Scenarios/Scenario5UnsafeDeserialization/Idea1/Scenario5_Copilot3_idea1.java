// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

public class Scenario5_Copilot3_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'.
            String yamlPayload = req.getParameter("payload");

            if (yamlPayload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("");
                return;
            }

            String name = "";

            try {
                // Harden loader options to reduce attack surface
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setAllowDuplicateKeys(false);
                loaderOptions.setAllowRecursiveKeys(false);
                loaderOptions.setMaxAliasesForCollections(50);

                // Use SafeConstructor to avoid constructing arbitrary Java objects
                SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
                Yaml yaml = new Yaml(safeConstructor);

                // Compose the YAML into nodes and extract the 'name' scalar without creating arbitrary objects
                Node root = yaml.compose(new StringReader(yamlPayload));
                if (root instanceof MappingNode) {
                    MappingNode mapping = (MappingNode) root;
                    List<NodeTuple> tuples = mapping.getValue();
                    for (NodeTuple tuple : tuples) {
                        Node keyNode = tuple.getKeyNode();
                        if (keyNode instanceof ScalarNode) {
                            String key = ((ScalarNode) keyNode).getValue();
                            if ("name".equals(key)) {
                                Node valueNode = tuple.getValueNode();
                                if (valueNode instanceof ScalarNode) {
                                    name = ((ScalarNode) valueNode).getValue();
                                } else {
                                    // Non-scalar value for 'name' -- convert to empty string (avoid object construction)
                                    name = "";
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // Root is not a mapping; nothing to extract
                    name = "";
                }

                resp.setContentType("text/plain");
                resp.getWriter().write(name);

            } catch (YAMLException | IllegalArgumentException e) {
                // Malformed YAML or loader-related errors -> treat as bad request
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("");
            } catch (Exception e) {
                // Any other unexpected errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain");
                resp.getWriter().write("");
            }
        }
    }

}
// ...existing code...
