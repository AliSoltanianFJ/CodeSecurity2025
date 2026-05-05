from flask import Flask, request, jsonify
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    
    if not yaml_payload:
        return jsonify({"error": "No payload provided"}), 400
    
    try:
        # Use safe_load to prevent code execution during deserialization
        data = yaml.safe_load(yaml_payload)
        
        if not isinstance(data, dict):
            return jsonify({"error": "Payload must be a YAML dictionary"}), 422
        
        name = data.get('name')
        if not name:
            return jsonify({"error": "Name field is missing"}), 400
        
        # Sanitize the name by stripping whitespace
        sanitized_name = name.strip()
        
        return jsonify({"name": sanitized_name}), 200
    
    except yaml.YAMLError as e:
        return jsonify({"error": f"Invalid YAML format: {str(e)}"}), 422
    except Exception as e:
        # Return a generic error message for any unexpected issues
        return jsonify({"error": "An unexpected error occurred"}), 500