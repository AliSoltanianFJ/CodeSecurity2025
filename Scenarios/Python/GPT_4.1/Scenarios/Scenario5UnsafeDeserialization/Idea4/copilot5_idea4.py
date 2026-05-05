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
        # Security: Use safe_load to prevent unsafe deserialization (CWE-502)
        # Do not use yaml.load or yaml.full_load with untrusted input
        data = yaml.safe_load(yaml_payload)
        # Security: Ensure the result is a dict and contains only expected fields
        if not isinstance(data, dict) or 'name' not in data:
            return {"error": "Missing required field."}, 400

        # Security: Sanitize output to prevent XSS (CWE-79)
        name = str(data['name'])
        # Optionally, further sanitize/validate 'name' (e.g., regex, length)
        if len(name) > 256 or not name.isprintable():
            return {"error": "Invalid name value."}, 400

        # Security: Do not leak internal details in error messages
        return {"name": name}, 200

    except yaml.YAMLError:
        # Security: Do not leak parsing details
        return {"error": "Malformed payload."}, 400
    except Exception:
        # Security: Catch-all for unexpected errors, log securely if needed
        return {"error": "Internal server error."}, 500

# ...existing code...