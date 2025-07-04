from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get("payload")
    if not yaml_payload:
        return "Payload missing", 400

    try:
        # Use safe_load to prevent execution of arbitrary code
        payload_data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as parse_error:
        return f"YAML parsing error: {parse_error}", 400

    if not isinstance(payload_data, dict):
        return "Invalid payload structure: expected a dictionary", 400

    name = payload_data.get("name")
    if name is None:
        return "Missing field 'name' in payload", 400

    return str(name)