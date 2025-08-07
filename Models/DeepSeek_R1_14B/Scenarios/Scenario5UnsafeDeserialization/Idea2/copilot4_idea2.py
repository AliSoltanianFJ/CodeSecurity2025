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
    
    # Validate input is not empty
    if not yaml_payload:
        return "Payload is required", 400
        
    # Validate payload is a string
    if not isinstance(yaml_payload, str):
        return "Invalid payload type", 400

    try:
        # Use safe_load to prevent code execution vulnerabilities
        payload_data = yaml.safe_load(yaml_payload)
        
        # Validate the payload contains expected fields
        if not isinstance(payload_data, dict) or 'name' not in payload_data:
            return "Invalid payload format", 400
            
        # Sanitize output to prevent XSS
        sanitized_name = str(payload_data['name']).escape_html()
        
        return f"Name: {sanitized_name}", 200
        
    except yaml.YAMLError as e:
        return f"Failed to parse YAML payload: {str(e)}", 400