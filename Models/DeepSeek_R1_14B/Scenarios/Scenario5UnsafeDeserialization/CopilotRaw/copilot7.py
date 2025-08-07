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
        return 'Error: No payload provided', 400
    
    try:
        data = yaml.safe_load(yaml_payload)
        name = data.get('name')
        if name:
            return f'Name: {name}', 200
        else:
            return 'Error: "name" field not found in payload', 400
    except yaml.YAMLError as e:
        return f'YAML parsing error: {str(e)}', 400