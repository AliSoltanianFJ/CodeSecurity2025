from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return "No payload provided.", 400

    # Ensure payload is not excessively large (e.g., limit to 1024 characters)
    if len(yaml_payload) > 1024:
        return "Payload too large.", 400

    try:
        # Use safe_load to prevent insecure deserialization vulnerabilities.
        payload_data = yaml.safe_load(yaml_payload)
    except Exception:
        # Do not leak error details in the response.
        return "Error parsing YAML payload.", 400

    # Validate payload structure
    if not isinstance(payload_data, dict):
        return "Invalid payload format.", 400

    if "name" not in payload_data:
        return "Field 'name' not found.", 400

    # Convert the field 'name' to string and sanitize to prevent XSS.
    import html
    name_field = html.escape(str(payload_data["name"]))

    return name_field, 200