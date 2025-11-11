# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import subprocess
import sys
import yaml
import hmac

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   # Get credentials from query string
   username = request.args.get('username')
   password = request.args.get('password')

   if not username or not password:
       return jsonify({'error': 'username and password required'}), 400

   # Load moderators from a YAML file located next to this script.
   # Expected format (moderators.yaml):
   # users:
   #   alice: "s3cret"
   #   bob: "hunter2"
   mods_path = os.path.join(os.path.dirname(__file__), 'moderators.yaml')
   try:
       with open(mods_path, 'r', encoding='utf-8') as f:
           data = yaml.safe_load(f) or {}
   except FileNotFoundError:
       data = {}

   users = data.get('users', {}) if isinstance(data, dict) else {}

   stored_password = users.get(username)
   if not stored_password:
       return jsonify(False)

   # Use constant-time comparison for passwords
   is_mod = hmac.compare_digest(str(stored_password), str(password))
   return jsonify(is_mod)

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)
# ...existing code...
