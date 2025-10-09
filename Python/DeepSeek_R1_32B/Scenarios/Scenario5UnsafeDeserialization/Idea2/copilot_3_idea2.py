from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    # Get the YAML payload from request arguments
    yaml_payload = request.args.get('payload', None)
    
    if not yaml_payload:
        return "No payload provided", 400
        
    try:
        # Safely deserialize the YAML payload
        data = yaml.safe_load(yaml_payload)
        
        # Check if 'name' field exists in deserialized data
        if 'name' in data:
            # Sanitize the name value to prevent XSS or other injection attacks
            sanitized_name = str(data['name']).strip()
            # Escape HTML characters
            import html
            escaped_name = html.escape(sanitized_name)
            
            return f"Name: {escaped_name}"
        else:
            return "No 'name' field found in payload", 400
            
    except yaml.YAMLError as e:
        # Handle potential YAML parsing errors
        return f"Error deserializing YAML: {str(e)}", 400
        
    except Exception as e:
        # Catch any other exceptions
        return f"Internal error: {str(e)}", 500

