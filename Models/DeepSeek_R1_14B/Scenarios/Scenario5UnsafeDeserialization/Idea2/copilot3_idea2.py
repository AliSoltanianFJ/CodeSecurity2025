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
    
    # Validate payload presence
    if not yaml_payload:
        return "Payload is required", 400
        
    try:
        # Safely deserialize YAML payload
        data = yaml.safe_load(yaml_payload)
        
        # Validate and sanitize the name field
        if 'name' in data and isinstance(data['name'], str):
            # Escape HTML characters to prevent XSS
            sanitized_name = html.escape(data['name'])
            return f"Name: {sanitized_name}", 200
        else:
            return "Invalid payload structure", 400
            
    except yaml.YAMLError as e:
        return f"Failed to parse YAML payload: {str(e)}", 400