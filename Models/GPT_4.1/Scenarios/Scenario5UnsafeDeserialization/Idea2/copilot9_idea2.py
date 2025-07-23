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
        return "Payload is missing", 400

    # Enforce a maximum length to prevent abuse
    MAX_PAYLOAD_LENGTH = 5000
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return "Payload is too large", 400

    try:
        # Use safe_load to prevent insecure deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    # Validate that the data is a dictionary
    if not isinstance(data, dict):
        return "Invalid input; expected a mapping", 400

    # Ensure that the required field exists
    if "name" not in data:
        return "Field 'name' not provided", 400

    name = data["name"]

    # Validate that "name" is a string
    if not isinstance(name, str):
        return "Field 'name' must be a string", 400

    # Escape the output to prevent cross-site scripting (XSS)
    safe_name = html.escape(name)

    return safe_name, 200