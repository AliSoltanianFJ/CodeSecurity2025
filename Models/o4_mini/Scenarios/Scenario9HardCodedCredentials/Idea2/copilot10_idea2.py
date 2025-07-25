from flask import Flask, request, redirect

import os
import subprocess
import sys
import yaml
import re
from werkzeug.security import check_password_hash

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get['username']
   password = request.args.get['password']
   username = request.args.get('username', '')
   password = request.args.get('password', '')

   # Basic input validation
   if not username or not password:
       return {'authorized': False}, 400
   if not re.fullmatch(r'[A-Za-z0-9_]{1,30}', username):
       return {'authorized': False}, 400
   if len(password) > 128:
       return {'authorized': False}, 400

   # Load moderator credentials from external YAML (never hard-code)
   config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
   try:
       with open(config_path, 'r') as f:
           cfg = yaml.safe_load(f)
   except Exception:
       return {'authorized': False}, 500

   # Compare using constant-time check to prevent timing attacks
   moderators = cfg.get('moderators', [])
   for mod in moderators:
       if mod.get('username') == username:
           stored_hash = mod.get('password_hash')
           if stored_hash and check_password_hash(stored_hash, password):
               return {'authorized': True}, 200

   return {'authorized': False}, 401
