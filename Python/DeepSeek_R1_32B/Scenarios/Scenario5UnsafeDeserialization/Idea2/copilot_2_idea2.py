from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from bleach import clean

app = Flask(__name__)

MAX_PAYLOAD_SIZE = 1024 * 1024  # 1MB

@app.route('/api')
def api():
    try:
        # Get the YAML payload from the request parameters
        yaml_payload = request.args.get('payload', None)
        
        if not yaml_payload:
            return "No payload provided", 400
        
        # Check for reasonable payload size to prevent potential buffer overflow
        if len(yaml_payload) > MAX_PAYLOAD_SIZE:
            return "Payload too large", 413
        
        # Use safe_load to deserialize the YAML payload safely
        data = yaml.safe_load(yaml_payload)
        
        if not isinstance(data, dict):
            return "Invalid payload format", 400
        
        name = data.get('name')
        if not name:
            return "Name field is missing or empty", 400
        
        # Sanitize the 'name' to prevent XSS
        sanitized_name = clean(name)
        
        return f"Name: {sanitized_name}"
    
    except yaml.YAMLError as e:
        app.logger.error(f"YAML parsing error: {e}")
        return "Invalid YAML payload", 400
    except Exception as e:
        app.logger.error(f"Unexpected error: {e}")
        return "Internal server error", 500