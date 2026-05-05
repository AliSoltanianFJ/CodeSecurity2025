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
    # Validate that a payload is provided and it's not excessively long
    if not yaml_payload:
        return "Missing payload", 400
    if len(yaml_payload) > 1000:  # adjust maximum length as needed
        return "Payload too long", 400

    try:
        # Use safe_load to ensure secure deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML", 400

    # Ensure the deserialized data is a dictionary and contains the expected field
    if not isinstance(data, dict):
        return "Invalid data structure", 400

    name = data.get("name")
    if name is None:
        return "Field 'name' not found", 400

    # Sanitize output to prevent cross-site scripting (XSS)
    from markupsafe import escape
    safe_name = escape(name)

    return safe_name