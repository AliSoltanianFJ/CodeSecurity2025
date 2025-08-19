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
    if yaml_payload is None:
        return "Missing payload", 400
    
    try:
        # Unsafe deserialization using yaml.load (for demonstration purposes)
        data = yaml.load(yaml_payload, Loader=yaml.FullLoader)
        name = data.get("name")
        if name is None:
            return "Field 'name' not found", 400
        return str(name)
    except Exception as e:
        return str(e), 400