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
        return "No payload provided.", 400

    # Limit payload size to prevent potential denial-of-service attacks.
    if len(yaml_payload) > 1000:  # adjust maximum length as needed
        return "Payload too large.", 400

    try:
        # Use safe_load to avoid insecure deserialization vulnerabilities.
        data = yaml.safe_load(yaml_payload)
    except Exception:
        # Do not leak error details in the response.
        return "Error parsing YAML payload.", 400

    if not isinstance(data, dict):
        return "Invalid payload format.", 400

    name_field = data.get("name")
    if name_field is None:
        return "Field 'name' not found.", 400
    # Ensure the field's value is of expected type and sanitized.
    if not isinstance(name_field, str):
        name_field = str(name_field)
    
    return name_field