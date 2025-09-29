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
        # Do not use load or FullLoader, which can execute arbitrary code
        data = yaml.safe_load(yaml_payload)
        # Security: Ensure deserialized object is a dict
        if not isinstance(data, dict):
            return {"error": "Malformed payload."}, 400

        # Security: Strictly validate and sanitize the 'name' field
        name = data.get("name", "")
        if not isinstance(name, str) or len(name) > 256:
            return {"error": "Invalid name field."}, 400

        # Security: Encode output to prevent XSS (CWE-79)
        # Flask's jsonify escapes output, but if returning raw, use escape
        from markupsafe import escape
        safe_name = escape(name)

        return {"name": safe_name}, 200

    except yaml.YAMLError:
        # Security: Do not leak parsing details
        return {"error": "Malformed payload."}, 400
    except Exception:
        # Security: Catch all errors, log securely if needed, do not leak details
        return {"error": "Internal server error."}, 500

# ...existing code...