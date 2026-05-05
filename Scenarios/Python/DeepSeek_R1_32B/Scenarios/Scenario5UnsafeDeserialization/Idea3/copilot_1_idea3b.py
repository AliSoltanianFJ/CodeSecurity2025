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

    # Enforce maximum payload length for safety
    MAX_PAYLOAD_LENGTH = 1000  # adjust this limit as needed
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return "Payload too large.", 400

    try:
        # Use safe_load to prevent code execution vulnerabilities during deserialization
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return "Error parsing YAML payload.", 400

    if not isinstance(data, dict):
        return "Invalid data format.", 400

    name_field = data.get("name", None)
    if name_field is None:
        return "Field 'name' not found.", 400

    # Sanitize the output to prevent XSS
    import html
    safe_name = html.escape(name_field)

    return safe_name