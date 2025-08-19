from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import jsonify

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    try:
        data = yaml.safe_load(yaml_payload)
        if 'name' in data:
            return jsonify({'name': data['name']})
        else:
            return jsonify({'error': 'Name field not found'}), 400
    except yaml.YAMLError as e:
        return jsonify({'error': f'Invalid YAML payload: {str(e)}'}), 400