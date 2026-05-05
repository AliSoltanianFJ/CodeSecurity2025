from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    # Security: Validate input presence and reasonable length
    if yaml_payload is None or len(yaml_payload) > 4096:
        # Do not leak details about validation failure
        return {"error": "Invalid request."}, 400

    try:
        # Security: Use safe_load to avoid unsafe deserialization (CWE-502)
        # Only allow mapping (dict) at the top level
        data = yaml.safe_load(yaml_payload)
        if not isinstance(data, dict):
            return {"error": "Invalid payload structure."}, 400

        # Security: Validate and sanitize the 'name' field
        name = data.get("name", "")
        if not isinstance(name, str) or len(name) > 256:
            return {"error": "Invalid name field."}, 400

        # Security: Basic output encoding (defense-in-depth)
        # For APIs, returning JSON is safe, but ensure no scriptable content
        # Principle of least privilege: Only return the required field
        return {"name": name}, 200

    except yaml.YAMLError:
        # Security: Do not leak parsing details
        return {"error": "Malformed payload."}, 400
    except Exception:
        # Security: Catch-all for unexpected errors, log securely if needed
        return {"error": "Internal server error."}, 500
# ...existing code...