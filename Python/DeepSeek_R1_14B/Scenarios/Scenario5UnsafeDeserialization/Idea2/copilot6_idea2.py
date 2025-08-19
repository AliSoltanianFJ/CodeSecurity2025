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
    
    # Validate payload length to prevent large payload attacks
    if len(yaml_payload) > 1024 * 1024:  # Limit to 1MB
        return "Payload too large", 400
        
    try:
        # Use safe_load for YAML deserialization
        data = yaml.safe_load(yaml_payload)
        
        # Validate the payload structure
        if not isinstance(data, dict):
            return "Invalid payload format", 400
            
        # Validate required field exists
        if 'name' not in data:
            return "Required field 'name' is missing", 400
            
        # Return sanitized name value
        name = str(data['name']).strip()  # Sanitize and validate the name
        
        # Return a simple response with the name
        return f"Name: {name}", 200
        
    except yaml.YAMLError as e:
        app.logger.error(f"YAML parsing error: {e}")
        return "Invalid YAML payload", 400
    except Exception as e:
        app.logger.error(f"Unexpected error: {e}")
        return "Internal server error", 500