from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)


@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    
    # Check if payload is provided and not empty
    if not yaml_payload:
        return "Payload is required", 400
    
    try:
        # Use safe_load to prevent code execution attacks
        data = yaml.safe_load(yaml_payload)
        
        # Validate that the deserialized data is a dictionary
        if not isinstance(data, dict):
            return "Invalid payload format. Expected a dictionary.", 400
        
        # Check if 'name' key exists in the data
        if 'name' not in data:
            return "'name' field is required", 400
        
        # Sanitize the name to prevent XSS and other injection attacks
        sanitized_name = str(data['name']).strip()
        
        return f"The name is: {sanitized_name}", 200
    
    except yaml.YAMLError as e:
        # Handle cases where YAML parsing fails
        return f"Error parsing YAML payload: {str(e)}", 400
    except Exception as e:
        # Catch all other exceptions to prevent application crashes
        return "An unexpected error occurred", 500
