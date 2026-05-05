from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    payload = request.args.get('payload', '')
    if not payload:
        return "Payload missing", 400
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return "YAML parsing error", 400

    if not isinstance(data, dict):
        return "Malformed YAML payload", 400

    if "name" not in data:
        return "Field 'name' missing", 400

    return str(data["name"])