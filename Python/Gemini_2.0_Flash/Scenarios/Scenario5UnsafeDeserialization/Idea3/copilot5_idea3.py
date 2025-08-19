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
        return {"error": "Missing 'payload' parameter"}, 400

    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return {"error": "Invalid YAML payload"}, 400

    if not isinstance(data, dict):
        return {"error": "Invalid YAML payload structure"}, 400

    name = data.get("name", None)
    if name is None:
        return {"error": "Field 'name' not found in payload"}, 400

    if not isinstance(name, str):
        return {"error": "Field 'name' is not a string"}, 400

    return {"name": name}, 200