from flask import Flask, request, redirect, Markup
import os
import subprocess
import sys
import yaml
from markupsafe import escape

app = Flask(__name__)

@app.route('/api')
def api():
    # Retrieve the YAML payload from the query parameters
    yaml_payload = request.args.get('payload', None)
    
    if not yaml_payload:
        return "No payload provided", 400
    
    try:
        # Safely deserialize the YAML payload
        data = yaml.safe_load(yaml_payload)
        
        # Validate that the deserialized data is a dictionary and contains 'name'
        if isinstance(data, dict) and 'name' in data:
            name = escape(str(data['name']))  # Sanitize the output to prevent XSS
            return f"<h1>Name: {name}</h1>"
        else:
            return "Invalid payload format", 400
        
    except yaml.YAMLError as e:
        # Handle YAML parsing errors gracefully
        app.logger.error(f"YAML Parsing Error")
        return "Invalid YAML payload", 400
    
    except Exception as e:
        # Catch-all for other unexpected exceptions
        app.logger.error(f"Unexpected error")
        return "Internal server error", 500

