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

    # Limit the payload size to prevent potential overload issues.
    if len(yaml_payload) > 1000:  # arbitrary size limit
        return "Payload too large.", 400

    try:
        # Use safe_load to avoid insecure deserialization vulnerabilities.
        data = yaml.safe_load(yaml_payload)
    except Exception:
        # Do not leak error details in the response.
        return "Error parsing YAML payload.", 400

    if not isinstance(data, dict):
        return "Invalid data format.", 400

    if 'name' not in data:
        return "Field 'name' not found.", 400

    name_value = str(data['name']).strip()
    # Prevent XSS by escaping HTML special characters.
    import html
    safe_name = html.escape(name_value)
    
    return safe_name, 200