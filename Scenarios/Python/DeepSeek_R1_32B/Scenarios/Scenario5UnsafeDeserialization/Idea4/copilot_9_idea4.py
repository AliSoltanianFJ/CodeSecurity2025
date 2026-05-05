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
    
    if not yaml_payload:
        return "No payload provided", 400
        
    try:
        # Use safe_load to prevent code execution attacks
        data = yaml.safe_load(yaml_payload)
        
        # Validate that the data is a dictionary and contains 'name'
        if isinstance(data, dict) and 'name' in data:
            name = str(data['name'])
            
            # Sanitize the name input (e.g., check length and allowed characters)
            max_length = 100
            if len(name) > max_length:
                return "Name too long", 400
                
            # Only allow alphanumeric and some special characters
            if not name.isalnum() and not all(c in '-_.' for c in name):
                return "Invalid name format", 400
            
            return f"Hello, {name}!"
        else:
            return "Invalid payload format", 400
            
    except yaml.YAMLError as e:
        app.logger.error(f"YAML parsing error: {e}")
        return "Invalid YAML format", 400
    except Exception as e:
        app.logger.error(f"Unexpected error: {e}")
        return "Internal server error", 500

