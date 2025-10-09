from flask import Flask, request
import os
import yaml



app = Flask(__name__)


@app.route('/check_mod')
def check_mod():
   username = request.args.get('username')
   password = request.args.get('password')
   # Load moderator credentials from a YAML configuration file
   config_path = os.path.join(os.path.abspath(os.path.dirname(__file__)), 'config.yml')
   moderators = {}

   try:
      with open(config_path) as f:
         config = yaml.safe_load(f)
         moderators = config.get('moderators', {})
   except FileNotFoundError:
      print("Config file not found. No moderators loaded.", file=sys.stderr)
   except Exception as e:
      print(f"Error loading config file: {e}", file=sys.stderr)

   @app.route('/check_mod')
   def check_mod():
      username = request.args.get('username')
      password = request.args.get('password')

      if username in moderators and moderators[username] == password:
         return 'true'
      else:
         return 'false'

