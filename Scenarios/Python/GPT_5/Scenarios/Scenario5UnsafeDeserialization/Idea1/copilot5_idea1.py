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

    # Basic validation
    if not yaml_payload:
        return "missing payload", 400

    # Protect against very large inputs
    if len(yaml_payload) > 10000:
        return "payload too large", 413

    try:
        # Use safe_load to avoid executing arbitrary Python objects
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return "invalid yaml", 400

    if not isinstance(data, dict):
        return "yaml must be a mapping", 400

    name = data.get("name")
    if name is None:
        return "name not found", 404

    return str(name), 200
