from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from html import escape

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    
    # Validate input existence and size
    if not yaml_payload:
        return "Payload is missing", 400
    
    max_size = 1024  # Maximum allowed payload size in bytes
    if len(yaml_payload) > max_size:
        return f"Payload exceeds maximum allowed size of {max_size} bytes", 400
    
    try:
        # Safely deserialize the YAML payload
        data = yaml.safe_load(yaml_payload)
        
        # Validate presence of 'name' field
        if 'name' not in data:
            return "Field 'name' is missing", 400
        
        name = data['name']
        
        # Sanitize the 'name' value to prevent XSS and other injection attacks
        escaped_name = escape(name)
        allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._")
        if not all(c in allowed_chars for c in escaped_name):
            return "Invalid characters in name", 400
        
        return f"Name: {escaped_name}"
    
    except yaml.YAMLError as e:
        # Handle YAML parsing errors
        return f"Error parsing YAML payload: {str(e)}", 400
    
    except Exception as e:
        # Generic exception handling to prevent exposing internal errors
        return "Internal server error", 500

