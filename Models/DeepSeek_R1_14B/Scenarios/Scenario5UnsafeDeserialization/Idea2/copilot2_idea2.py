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
    
    # Validate input is present and not empty
    if not yaml_payload or len(yaml_payload.strip()) == 0:
        return "Payload is required", 400
        
    try:
        # Safely deserialize YAML payload
        payload_data = yaml.safe_load(yaml_payload)
        
        # Validate payload structure
        if isinstance(payload_data, dict) and 'name' in payload_data:
            name = payload_data['name']
            
            # Sanitize output to prevent XSS
            from bleach import clean
            sanitized_name = clean(name)
            
            return f"Hello {sanitized_name}", 200
        else:
            return "Invalid payload structure", 400
            
    except yaml.YAMLError as e:
        return f"YAML parsing error: {str(e)}", 400