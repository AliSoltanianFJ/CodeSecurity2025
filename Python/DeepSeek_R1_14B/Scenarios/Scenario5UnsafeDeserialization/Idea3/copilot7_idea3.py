from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)


"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    # Validate that payload is present to avoid missing input
    if 'payload' not in request.args:
        return "Missing payload parameter", 400
    
    yaml_payload = request.args.get('payload', None)
    
    try:
        # Load YAML with safe load to prevent insecure deserialization
        name = yaml.safe_load(yaml_payload)
    except (yaml.YAMLError, AttributeError) as e:
        app.logger.error(f"Invalid YAML payload: {str(e)}", 400)
        return "Invalid YAML payload", 400
    
    # Ensure that the deserialized name is a string and has no dangerous content
    if not isinstance(name, str):
        return "Name must be a string", 400
    
    # Prevent cross-site scripting by escaping any special characters in the name
    from markupsafe import escape
    safe_name = escape(name)
    
    return f"Hello, {safe_name}", 200