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
        return "No payload provided.", 400

    try:
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return "Invalid YAML payload.", 400

    if not isinstance(data, dict):
        return "Payload is not a dictionary.", 400

    if "name" not in data:
        return "Field 'name' not found in payload.", 400

    name_field = data.get("name", "")
    if not isinstance(name_field, str):
        return "'name' field is not a string.", 400

    return name_field