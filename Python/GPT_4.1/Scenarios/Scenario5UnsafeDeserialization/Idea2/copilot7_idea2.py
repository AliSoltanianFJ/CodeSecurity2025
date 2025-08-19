from flask import Flask, request
from markupsafe import escape
import yaml

app = Flask(__name__)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return "Missing payload", 400

    # Limit payload size to prevent potential DoS attacks
    if len(yaml_payload) > 1000:
        return "Payload too large", 400

    try:
        # Use safe_load to prevent code execution vulnerabilities
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    # Ensure the deserialized object is a dict and contains the expected key "name"
    if not isinstance(data, dict):
        return "Payload structure invalid", 400

    name = data.get("name")
    if not isinstance(name, str):
        return "Field 'name' missing or invalid", 400

    # Sanitize the 'name' to prevent XSS issues
    safe_name = escape(name)

    return safe_name, 200