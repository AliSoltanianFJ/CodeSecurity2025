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
    if not yaml_payload:
        return "missing payload", 400
    try:
        # Use safe_load to avoid arbitrary object instantiation
        data = yaml.safe_load(yaml_payload)
    except Exception as e:
        return f"invalid yaml: {e}", 400

    if not isinstance(data, dict):
        return "payload must be a mapping", 400

    name = data.get("name")
    if name is None:
        return "name not found", 404

    return str(name)