from flask import Flask, request, redirect
import os
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    # Retrieve the environment variable or use default file path
    mod_credentials_path = os.getenv('MOD_CREDENTIALS', 'mod_credentials.yml')
    
    # Read the credentials from the specified source
    if os.path.isfile(mod_credentials_path):
        with open(mod_credentials_path, 'r') as f:
            try:
                moderators = yaml.safe_load(f)
            except yaml.YAMLError as e:
                app.logger.error(f"Error loading YAML file: {e}")
                return "false"
    else:
        # Attempt to get credentials from environment variable
        mod_creds_env = os.getenv('MOD_CREDENTIALS_CONTENT', None)
        if mod_creds_env:
            try:
                moderators = yaml.safe_load(mod_creds_env)
            except yaml.YAMLError as e:
                app.logger.error(f"Error loading YAML from env: {e}")
                return "false"
        else:
            app.logger.error("No valid credentials source found")
            return "false"
    
    # Retrieve username and password correctly using get()
    username = request.args.get('username')
    password = request.args.get('password')
    
    if isinstance(moderators, dict):
        if username in moderators and moderators[username] == password:
            return "true"
    return "false"

