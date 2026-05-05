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

    # Ensure payload is not excessively large (e.g., limit to 1kb)
    if len(yaml_payload) > 1024:
        return "Payload too large.", 400

    try:
        # Use safe_load to prevent insecure deserialization vulnerabilities.
        data = yaml.safe_load(yaml_payload)
    except Exception:
        # Do not leak error details in the response.
        return "Error parsing YAML payload.", 400

    if not isinstance(data, dict):
        return "Invalid data format.", 400

    name = data.get("name")
    if name is None:
        return "Field 'name' not found.", 400

    # Sanitize output to prevent XSS.
    from html import escape
    safe_name = escape(name)
    return f"<p>{safe_name}</p>", 200