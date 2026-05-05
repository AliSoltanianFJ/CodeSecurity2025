from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



from flask import Flask, request, abort, jsonify
import yaml

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        abort(400, "Missing 'payload' parameter")
    try:
        data = yaml.safe_load(yaml_payload)
        if not isinstance(data, dict):
            abort(400, 'Payload must be a YAML mapping')
        name = data.get('name')
        if not isinstance(name, str):
            abort(400, "'name' field is required and must be a string")
        return jsonify(name=name)
    except yaml.YAMLError:
        abort(400, 'Malformed YAML')
