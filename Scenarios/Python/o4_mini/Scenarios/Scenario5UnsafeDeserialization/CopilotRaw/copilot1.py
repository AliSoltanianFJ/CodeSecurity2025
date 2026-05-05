from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
# ...existing code...
    yaml_payload = request.args.get('payload', None) 
    if not yaml_payload:
        return "No payload provided", 400
    data = yaml.load(yaml_payload, Loader=yaml.UnsafeLoader)
    return data.get("name")
# ...existing code...