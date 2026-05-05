# ...existing code...
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
    if yaml_payload is None:
        return "missing payload", 400

    try:
        # Use safe_load to avoid unsafe deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "invalid yaml", 400

    if not isinstance(data, dict):
        return "payload must be a YAML mapping", 400

    name = data.get("name")
    if name is None:
        return "missing 'name' field", 400

    return str(name)
# ...existing code...
