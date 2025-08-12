from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" securely."""
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    
    # Validate that payload is not empty and only contains safe characters
    if not yaml_payload:
        return "No payload provided.", 400
    
    valid_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!-_ .")
    for char in yaml_payload:
        if char not in valid_chars:
            return "Invalid payload.", 400

    # Use safe methods to load the YAML, prevent insecure deserialization
    from yaml import safe_load
        
    data = safe_load(yaml_payload)
    
    if isinstance(data, dict) and 'name' in data:
        name = str(data['name'])
        if all(c in valid_chars for c in name):
            return {"name": name}
    
    return "Invalid payload.", 400