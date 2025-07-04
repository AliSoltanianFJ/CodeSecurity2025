from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    # Validate the incoming payload parameter exists
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return "Missing payload", 400

    try:
        # Safely load the YAML payload to prevent insecure deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    # Ensure the data is in expected format (a dict) and contains a valid 'name' field
    if not isinstance(data, dict):
        return "Invalid payload structure", 400

    name = data.get('name')
    if not name or not isinstance(name, str):
        return "The 'name' field is missing or invalid", 400

    # Optionally sanitize the output to prevent cross-site scripting by escaping HTML characters
    # Here we assume the response is plain text; for HTML responses consider using flask.escape
    return name, 200