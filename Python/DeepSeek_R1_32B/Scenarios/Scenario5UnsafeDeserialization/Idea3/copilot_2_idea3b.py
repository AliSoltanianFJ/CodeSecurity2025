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

    # Limit payload size to avoid potential DoS attacks
    if len(yaml_payload) > 1000:  # arbitrary size limit
        return "Payload too large", 400

    try:
        # Use safe_load to prevent code execution vulnerabilities during deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML format", 400

    if not isinstance(data, dict):
        return "Payload structure invalid", 400

    if 'name' not in data:
        return "Field 'name' not found", 400

    name_value = str(data['name']).strip()
    # Sanitize to prevent XSS
    from markupsafe import escape
    sanitized_name = escape(name_value)
    
    return sanitized_name, 200