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
        return "No YAML payload provided", 400

    try:
        # Safely deserialize the YAML payload
        parsed_data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as parse_err:
        return f"YAML parsing error: {parse_err}", 400

    if not (isinstance(parsed_data, dict) and "name" in parsed_data):
        return "YAML payload must be a mapping containing a 'name' field", 400

    return str(parsed_data["name"])