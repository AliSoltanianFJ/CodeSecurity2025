from flask import Flask, request, redirect
import os
import yaml
import bcrypt

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    # Path to the configuration file containing moderator credentials
    config_path = os.getenv('MOD_CONFIG_PATH', 'moderators.yml')

    try:
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
            moderators = config.get('moderators', {})
            
            if username not in moderators:
                return 'false'
            
            # Retrieve the stored hash
            stored_hash = moderators[username].encode('utf-8')
            
            # Check the password against the stored hash
            if bcrypt.checkpw(password.encode('utf-8'), stored_hash):
                return 'true'
            else:
                return 'false'
    except (IOError, yaml.YAMLError) as e:
        # Log the error if necessary and return false as a failsafe
        return 'false'
