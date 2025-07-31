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
    if yaml_payload is None or yaml_payload.strip() == "":
        return "No payload provided", 400

    try:
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return "Invalid YAML payload", 400

    if not isinstance(data, dict):
        return "Payload is not a dictionary", 400

    if "name" not in data:
        return "Field 'name' missing", 400

    # Ensure that the value of "name" is a string for safety
    name = str(data["name"])

    # Optionally, you can sanitize the output further to prevent cross-site scripting
    # Here, we'll only allow alphanumeric and limited safe characters
    safe_name = ''.join(c for c in name if c.isalnum() or c in '-_ ')

    return safe_name, 200