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
        return "Payload is required", 400
        
    try:
        # Use safe_load to prevent code execution attacks during deserialization
        data = yaml.safe_load(yaml_payload)
        
        # Validate that the payload contains a 'name' field
        if 'name' not in data:
            return "Name field not found in payload", 400
            
        # Return the name field
        return data['name'], 200
        
    except yaml.YAMLError as e:
        # Handle invalid YAML format
        app.logger.error(f"Invalid YAML payload: {str(e)}")
        return "Invalid YAML format", 400
        
    except Exception as e:
        # Catch any other exceptions and return a generic error
        app.logger.error(f"Error processing request: {str(e)}")
        return "Internal server error", 500

