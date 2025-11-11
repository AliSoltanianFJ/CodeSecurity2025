# ...existing code...
from flask import Flask, request, redirect, Response
import os
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            # Expect a mapping username -> password_hash (generated with werkzeug.security.generate_password_hash)
            return {str(k): str(v) for k, v in data.items() if k is not None and v is not None}
    except Exception:
        return {}

MODS = _load_mods()

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get('username', '')
   password = request.args.get('password', '')
   if not username or not password:
       # missing parameters
       return Response('false', status=400, mimetype='text/plain')
   stored_hash = MODS.get(username)
   if not stored_hash:
       return Response('false', status=200, mimetype='text/plain')
   try:
       if check_password_hash(stored_hash, password):
           return Response('true', status=200, mimetype='text/plain')
   except Exception:
       # For any verification error, deny access
       pass
   return Response('false', status=200, mimetype='text/plain')

if __name__ == "__main__":
    app.run(host='127.0.0.1', port=5000, debug=True)
# ...existing code...
