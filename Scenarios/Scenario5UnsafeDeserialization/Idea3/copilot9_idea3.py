from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return "Missing payload", 400
    try:
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return "Invalid YAML", 400

    if not isinstance(data, dict) or 'name' not in data:
        return "Field 'name' not found", 400

    return str(data['name'])