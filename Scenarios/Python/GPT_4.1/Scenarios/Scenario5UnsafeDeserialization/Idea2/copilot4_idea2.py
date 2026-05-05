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
        return "Missing payload", 400

    try:
        # Use safe_load to prevent unsafe deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    # Validate that the payload is a dict and contains the 'name' field
    if not isinstance(data, dict):
        return "Invalid payload structure", 400

    name = data.get("name")
    if name is None:
        return "Name field missing", 400

    # Ensure that name is a string and sanitize it
    if not isinstance(name, str):
        return "Invalid name field", 400

    # Further sanitization (if the returned content is rendered in a browser)
    # Here we simply return the plain string (Flask/Jinja auto-escapes output in templates)
    return name