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
    # Validate input presence
    if yaml_payload is None:
        return "Missing payload", 400

    try:
        # Securely parse YAML input
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return "Invalid YAML", 400

    # Ensure the deserialized object is a dictionary and contains 'name'
    if not isinstance(data, dict):
        return "Invalid data format", 400

    name_value = data.get("name")
    if name_value is None:
        return "'name' field not found", 400

    # Safely render the extracted name value
    return str(name_value)