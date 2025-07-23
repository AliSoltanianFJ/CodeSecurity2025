from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    payload = request.args.get('payload')
    if not payload:
        return "No YAML payload provided", 400

    try:
        parsed_data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return "Failed to parse YAML", 400

    if not isinstance(parsed_data, dict):
        return "Payload must be a YAML mapping", 400

    name_field = parsed_data.get("name")
    if name_field is None:
        return "The 'name' field is missing", 400

    return str(name_field)