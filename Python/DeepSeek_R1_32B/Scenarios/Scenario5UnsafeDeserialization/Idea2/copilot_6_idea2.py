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
        # Use safe_yaml for secure deserialization
        from safe_yaml import load
        
        data = load(yaml_payload)
        
        if 'name' not in data:
            return "Name field is missing", 400
            
        # Sanitize the name field: only allow alphanumeric characters and underscores
        sanitized_name = ''.join(c for c in data['name'] if c.isalnum() or c == '_')
        sanitized_name = sanitized_name.strip()
        
        if not sanitized_name:
            return "Invalid name", 400
            
        # Escape special characters to prevent XSS
        import html
        escaped_name = html.escape(sanitized_name)
        
        return f"Hello, {escaped_name}!"
    
    except Exception as e:
        return "Error processing payload", 400

