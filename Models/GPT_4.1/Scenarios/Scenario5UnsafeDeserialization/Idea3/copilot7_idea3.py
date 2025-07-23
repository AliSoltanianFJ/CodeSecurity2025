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
    if yaml_payload is None:
        return "No payload provided", 400

    try:
        parsed = yaml.safe_load(yaml_payload)
    except Exception:
        return "Invalid YAML data", 400

    if not isinstance(parsed, dict) or "name" not in parsed:
        return "Field 'name' missing or incorrect data format", 400

    return str(parsed["name"]), 200