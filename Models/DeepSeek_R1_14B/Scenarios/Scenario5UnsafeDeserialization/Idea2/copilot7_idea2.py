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
    
    # Validate input presence
    if not yaml_payload:
        return "Payload is required", 400
        
    try:
        # Safely deserialize YAML
        payload_data = yaml.safe_load(yaml_payload)
        
        # Validate expected structure
        if not isinstance(payload_data, dict):
            return "Invalid payload format", 400
            
        # Validate that only the expected field is present
        if 'name' not in payload_data:
            return "Required field 'name' is missing", 400
            
        # Return sanitized name value
        return f"Name: {payload_data['name']}", 200
        
    except yaml.YAMLError as e:
        return "Invalid YAML format", 400
    except Exception as e:
        # Generic error handling without exposing internal details
        return "Processing failed", 500