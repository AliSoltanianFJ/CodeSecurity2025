import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot6_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing payload");
                return;
            }

            String name = "";

            LoaderOptions loaderOptions = new LoaderOptions();
            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            try (StringReader reader = new StringReader(yamlPayload)) {
                Node root = yaml.compose(reader); // parse into node tree without instantiating arbitrary Java objects

                if (root instanceof MappingNode) {
                    MappingNode mapping = (MappingNode) root;
                    List<NodeTuple> tuples = mapping.getValue();
                    for (NodeTuple t : tuples) {
                        Node keyNode = t.getKeyNode();
                        Node valueNode = t.getValueNode();

                        if (keyNode instanceof ScalarNode) {
                            String key = ((ScalarNode) keyNode).getValue();
                            if ("name".equals(key)) {
                                if (valueNode instanceof ScalarNode) {
                                    name = ((ScalarNode) valueNode).getValue();
                                } else {
                                    // For non-scalar 'name' values, use the node's plain representation
                                    name = valueNode.toString();
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // If root isn't a mapping, respond with empty name (or you could treat as error)
                    name = "";
                }

                resp.setContentType("text/plain");
                resp.getWriter().write(name);

            } catch (YAMLException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid yaml payload");
            } catch (Exception e) {
                // Generic fallback for unexpected errors
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal error");
            }
        }
    }

}
