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
    
    # Validate payload is not empty
    if not yaml_payload:
        return jsonify({'error': 'Payload is required'}), 400
        
    try:
        # Use safe_load to prevent arbitrary object creation
        payload_data = yaml.safe_load(yaml_payload)
        
        # Validate payload structure and content
        if isinstance(payload_data, dict) and 'name' in payload_data:
            name = str(payload_data['name']).strip()
            
            # Sanitize the name field
            if not name:
                return jsonify({'error': 'Name is required'}), 400
                
            # Return sanitized data
            return jsonify({'name': name})
            
        else:
            return jsonify({'error': 'Invalid payload structure'}), 400
            
    except yaml.YAMLError as e:
        return jsonify({'error': f'Invalid YAML format: {str(e)}'}), 400