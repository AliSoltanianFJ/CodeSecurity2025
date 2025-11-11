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
    # ...existing code...
    if not yaml_payload:
        return "missing payload", 400

    try:
        # Use safe_load to avoid arbitrary object construction
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "invalid YAML", 400

    # Expecting a mapping with a 'name' field
    if not isinstance(data, dict):
        return "payload must be a YAML mapping", 400

    name = data.get("name")
    if name is None:
        return "name not found", 400

    if not isinstance(name, (str, int, float, bool)):
        return "invalid name type", 400

    # Ensure we return a string
    return str(name)
