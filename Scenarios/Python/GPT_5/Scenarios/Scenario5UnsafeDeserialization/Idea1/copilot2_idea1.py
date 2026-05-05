from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

# ...existing code...
"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None) 
    # Safely parse YAML using safe_load to avoid executing arbitrary objects
    if not yaml_payload:
        return "Missing 'payload' parameter", 400
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    if not isinstance(data, dict):
        return "YAML payload must be a mapping (dict)", 400

    name = data.get("name")
    if name is None:
        return "Field 'name' not found", 404

    return str(name), 200
# ...existing code...
