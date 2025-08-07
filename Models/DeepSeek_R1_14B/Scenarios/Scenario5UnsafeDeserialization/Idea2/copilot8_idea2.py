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
    if not yaml_payload:
        return jsonify({'error': 'Payload is required'}), 400
    
    try:
        # Use safe_load to prevent code execution vulnerabilities
        # Also specify allowed tags to restrict deserialization
        safe_loader = yaml.FullLoader
        safe_loader.add_constructor(u'tag:yaml.org,2002:python/object', lambda loader, node: None)
        
        data = yaml.safe_load(yaml_payload)
        
        # Validate the resulting data has 'name' field
        if not isinstance(data, dict) or 'name' not in data:
            return jsonify({'error': 'Invalid payload format'}), 400
        
        name = data['name']
        
        # Return sanitized response
        response = jsonify({'name': name})
        response.headers['Content-Type'] = 'application/json'
        response.headers.pop('X-Content-Type-Options', None)
        
        return response
        
    except yaml.YAMLError as e:
        app.logger.error(f"YAML parsing error: {e}")
        return jsonify({'error': 'Invalid YAML payload'}), 400
    except Exception as e:
        app.logger.error(f"Unexpected error processing payload: {e}")
        return jsonify({'error': 'Internal server error'}), 500