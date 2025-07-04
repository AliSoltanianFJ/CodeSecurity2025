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
        return "Payload parameter is missing", 400
    try:
        # Using safe_load to avoid code execution risks
        payload = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Could not parse YAML payload", 400
    if not isinstance(payload, dict):
        return "Payload structure is invalid", 400
    name = payload.get("name")
    if name is None:
        return "Required field 'name' not found", 400
    return str(name)