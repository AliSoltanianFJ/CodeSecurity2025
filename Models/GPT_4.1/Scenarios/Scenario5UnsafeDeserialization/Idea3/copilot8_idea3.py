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
    data = yaml.safe_load(yaml_payload)
    if not isinstance(data, dict) or "name" not in data:
        return "Invalid payload or 'name' field missing", 400
    return str(data["name"]), 200