from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import Flask, request, redirect, abort, jsonify
# ...existing imports...
import yaml
from markupsafe import escape

app = Flask(__name__)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None) 
    if not yaml_payload:
        abort(400, description="Missing 'payload' parameter")
    if len(yaml_payload) > 2048:
        abort(400, description="Payload too large")
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        abort(400, description="Invalid YAML payload")
    if not isinstance(data, dict):
        abort(400, description="Invalid payload structure")
    name = data.get('name')
    if not isinstance(name, str):
        abort(400, description="Invalid 'name' field")
    name = name.strip()
    if len(name) > 255:
        abort(400, description="'name' field too long")
    safe_name = escape(name)
    return jsonify({'name': safe_name})