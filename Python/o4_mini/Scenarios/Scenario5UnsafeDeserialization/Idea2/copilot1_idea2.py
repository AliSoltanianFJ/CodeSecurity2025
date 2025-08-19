from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """


from flask import Flask, request, redirect, abort, jsonify

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return abort(400, "Missing payload parameter")
    if len(yaml_payload) > 10000:
        return abort(400, "Payload too large")
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return abort(400, "Invalid YAML payload")
    if not isinstance(data, dict):
        return abort(400, "Invalid payload format")
    name = data.get("name")
    if not isinstance(name, str):
        return abort(400, "Invalid or missing 'name' field")
    safe_name = name[:256]
    return jsonify(name=safe_name)
