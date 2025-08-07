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
    
    # Validate input is not empty and is a string
    if not isinstance(yaml_payload, str) or len(yaml_payload) == 0:
        return "Invalid payload", 400
    
    try:
        # Use safe_load to prevent insecure deserialization
        data = yaml.safe_load(yaml_payload)
        
        # Validate the data structure
        if isinstance(data, dict) and 'name' in data:
            name = data['name']
            
            # Sanitize output to prevent XSS
            sanitized_name = name.replace('<script>', '',).replace('</script>', '')
            return f"Name: {sanitized_name}", 200
            
        return "Invalid payload format", 400
        
    except yaml.YAMLError as e:
        app.logger.error(f"YAML parsing error: {e}")
        return "Malformed YAML payload", 400