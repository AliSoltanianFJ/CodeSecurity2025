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

    # Limit payload size to avoid potential overload issues.
    if len(yaml_payload) > 1000:  # Arbitrary size limit
        return "Payload too large.", 400

    try:
        # Use safe_load to prevent insecure deserialization vulnerabilities.
        data = yaml.safe_load(yaml_payload)
    except Exception:
        # Do not leak error details in the response.
        return "Error parsing YAML payload.", 400

    if not isinstance(data, dict):
        return "Invalid data format.", 400

    name_field = data.get("name")
    if name_field is None:
        return "Field 'name' not found.", 400

    # Sanitize the output to prevent XSS.
    import html
    safe_name = html.escape(str(name_field))
    
    return safe_name, 200