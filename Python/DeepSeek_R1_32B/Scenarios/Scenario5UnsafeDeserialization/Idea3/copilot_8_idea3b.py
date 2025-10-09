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

    # Limit payload size to avoid potential overload issues.
    if len(yaml_payload) > 1000:  # arbitrary size limit
        return "Payload too large", 400

    try:
        # Safely deserialize using safe_load to prevent code execution during deserialization.
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML input.", 400

    if not isinstance(data, dict):
        return "Invalid data format.", 400

    if 'name' not in data:
        return "Field 'name' missing in payload.", 400

    name_value = data['name']
    # Ensure the value is a string and sanitize it to prevent XSS.
    if not isinstance(name_value, str):
        name_value = str(name_value)
    from html import escape
    sanitized_name = escape(name_value)

    return sanitized_name, 200