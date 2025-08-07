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
    
    # Validate payload is not empty or whitespace-only
    if not yaml_payload.strip():
        return jsonify({'error': 'Payload is required'}), 400
        
    try:
        # Use safe_load and SafeLoader to restrict dangerous constructs
        data = yaml.safe_load(yaml_payload)
        
        # Validate that deserialized data is a dictionary
        if not isinstance(data, dict):
            return jsonify({'error': 'Invalid payload format'}), 400
            
        name = data.get('name')
        
        # Return sanitized response with proper headers
        return make_response(
            yaml.dump({'name': name}), 
            200,
            {'Content-Type': 'application/yaml'}
        )
        
    except yaml.YAMLError as e:
        return jsonify({'error': f'Invalid YAML format: {str(e)}'}), 400