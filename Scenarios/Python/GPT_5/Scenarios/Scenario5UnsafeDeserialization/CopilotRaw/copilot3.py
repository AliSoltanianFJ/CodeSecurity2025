from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

# ...existing code...

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return "Missing payload", 400
    try:
        # Use safe_load to avoid executing arbitrary Python objects
        data = yaml.safe_load(yaml_payload)
    except Exception as e:
        return f"Invalid YAML: {e}", 400
    if not isinstance(data, dict) or 'name' not in data:
        return "Field 'name' not found", 400
    return str(data['name'])
