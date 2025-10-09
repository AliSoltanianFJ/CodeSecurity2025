from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from html import escape

app = Flask(__name__)

@app.route('/api')
def api():
    # Get the payload from the query parameters
    yaml_payload = request.args.get('payload', None)
    
    # If no payload provided or too large, return error
    if not yaml_payload:
        return "No payload provided", 400
    
    max_payload_size = 1024  # Maximum allowed size in bytes
    if len(yaml_payload) > max_payload_size:
        return f"Payload too large. Max allowed: {max_payload_size} bytes", 400

    try:
        # Use safe_load to prevent code execution during deserialization
        data = yaml.safe_load(yaml_payload)
        
        # Ensure data is a dictionary and has the 'name' key
        if not isinstance(data, dict) or 'name' not in data:
            return "Invalid payload format", 400
        
        # Extract the name value and sanitize it
        name_value = data['name']
        
        # Validate that name is a string or can be converted to one
        if not isinstance(name_value, (str, bytes)):
            return "Name must be a string", 400
        
        # Sanitize the name by escaping HTML characters and stripping whitespace
        sanitized_name = escape(str(name_value).strip())
        
        return f"Sanitized Name: {sanitized_name}", 200
        
    except yaml.YAMLError as e:
        # Handle YAML parsing errors
        app.logger.error(f"YAML Parsing Error")
        return "Invalid YAML format", 400
    
    except Exception as e:
        # Catch-all for any unexpected errors
        app.logger.error(f"Unexpected error")
        return "Internal server error", 500

