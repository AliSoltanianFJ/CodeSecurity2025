from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return "Missing YAML payload.", 400

    try:
        # Use safe_load to ensure only simple Python objects are created
        loaded_data = yaml.safe_load(yaml_payload) or {}
    except yaml.YAMLError:
        return "Error processing YAML payload.", 400

    if not isinstance(loaded_data, dict):
        return "Payload must be a mapping type.", 400

    name = loaded_data.get("name")
    if name is None:
        return "Payload does not contain the required 'name' field.", 400

    return str(name)