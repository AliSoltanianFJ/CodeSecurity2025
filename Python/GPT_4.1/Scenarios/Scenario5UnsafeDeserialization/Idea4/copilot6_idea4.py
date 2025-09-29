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

        # Security: Strictly validate and sanitize the "name" field
        name = data.get("name", None)
        if not isinstance(name, str) or len(name) > 256:
            return {"error": "Invalid name field."}, 400

        # Security: Output encoding (context-aware) for returned data
        # Flask's jsonify automatically escapes output
        from flask import jsonify
        return jsonify({"name": name}), 200

    except yaml.YAMLError:
        # Security: Do not leak parsing details
        return {"error": "Malformed payload."}, 400
    except Exception:
        # Security: Catch-all for unexpected errors, no sensitive info leaked
        return {"error": "Internal server error."}, 500

# ...existing code...