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
    if not yaml_payload:
        return "No payload provided", 400
    try:
        # Use safe_load to prevent any potential security issues with loading arbitrary YAML
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        app.logger.error(f"Error processing YAML payload: {e}")
        return f"YAML processing error", 500
    if "name" not in data or not isinstance(data["name"], str):
        return "Invalid payload format", 400
    # Always use bleach to clean any user input, especially for rendering to prevent XSS
    from bleach import clean
    name = clean(str(data.get("name")), tags=[], attributes=[], strip=True)
    if len(name) > 256:
        return "Name too long", 400
    return name