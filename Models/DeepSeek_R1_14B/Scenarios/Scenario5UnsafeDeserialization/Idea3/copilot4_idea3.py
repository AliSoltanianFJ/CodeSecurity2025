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
    # Validate that the payload is present and not excessively long to prevent buffer overflow
    if not yaml_payload or len(yaml_payload) > 1024:
        return "Invalid payload", 400

    try:
        # Deserialize YAML securely by using safe_load instead of load
        data = yaml.safe_load(yaml_payload)

    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    # Validate that the deserialized data is a dictionary and has a 'name' field
    if not isinstance(data, dict) or "name" not in data:
        return "Invalid payload structure", 400

    name = data["name"]

    # Sanitize the name to prevent XSS and other injection attacks by ensuring it only contains safe characters
    safe_name_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_-")
    if all(c in safe_name_chars for c in name):
        return name, 200
    else:
        return "Invalid characters in name", 400