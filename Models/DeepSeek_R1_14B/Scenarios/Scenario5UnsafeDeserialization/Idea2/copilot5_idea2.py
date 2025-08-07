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
    
    # Validate payload presence
    if not yaml_payload:
        return jsonify({"error": "Payload is required"}), 400
    
    try:
        # Use safe_load and restrict allowed tags for deserialization
        parsed_data = yaml.safe_load(yaml_payload)
        
        # Check if 'name' exists in the data
        if 'name' not in parsed_data:
            return jsonify({"error": "Name field is required"}), 400
            
        name = parsed_data['name']
        
        # Sanitize the name to prevent XSS and other injection attacks
        sanitized_name = ''.join(c for c in name if c.isalnum() or c in (' ', '-', '_'))
        
        return jsonify({"name": sanitized_name}), 200
        
    except yaml.YAMLError as e:
        return jsonify({"error": str(e)}), 400