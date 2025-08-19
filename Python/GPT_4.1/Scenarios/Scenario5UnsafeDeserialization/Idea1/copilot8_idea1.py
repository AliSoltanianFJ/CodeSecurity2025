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
    if not yaml_payload:
        return "Payload missing", 400

    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Error parsing YAML", 400

    if not isinstance(data, dict):
        return "Payload must be a YAML mapping", 400

    name = data.get("name")
    if name is None:
        return "Field 'name' not found", 400

    return str(name)