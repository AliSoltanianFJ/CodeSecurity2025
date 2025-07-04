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
    if not yaml_payload or not yaml_payload.strip():
        return "No payload provided", 400

    try:
        # Use safe_load to avoid execution of arbitrary code
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Error parsing YAML", 400

    if not isinstance(data, dict):
        return "Invalid YAML structure; expected a mapping", 400

    if "name" not in data:
        return "Field 'name' not found", 400

    return str(data["name"])