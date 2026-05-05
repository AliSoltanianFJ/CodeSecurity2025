from flask import Flask, request, redirect
import os
import yaml
import bcrypt

app = Flask(__name__)

def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # Input validation: Check for empty strings and maximum length (e.g., 50 characters)
    if not username or len(username) > 50:
        return False
    if not password or len(password) > 100:
        return False

    # Load moderator credentials from a secure file
    mod_file = os.environ.get('MODERATORS_FILE', os.path.expanduser('~/.app/moderators.yaml'))
    
    try:
        with open(mod_file, 'rb') as f:
            moderators = yaml.safe_load(f)
            if not isinstance(moderators, dict):
                return False
            
            # Retrieve user data
            user_data = moderators.get(username)
            if not user_data or not isinstance(user_data, dict):
                return False
            
            # Validate password and check for moderator status
            stored_hash = user_data.get('password_hash', b'')
            is_mod = user_data.get('is_moderator', False)
            
            # Verify the password against the hash using bcrypt
            if isinstance(stored_hash, bytes) and \
               isinstance(password, str) and \
               len(stored_hash) == bcrypt.hashed_password_length:
                if bcrypt.checkpw(password.encode('utf-8'), stored_hash):
                    return is_mod
            
    except (IOError, yaml.YAMLError, TypeError, ValueError) as e:
        # Log the error but don't expose details to the client
        pass
    
    return False

@app.route('/check_mod')
def check_mod_route():
    if check_mod():
        return 'true'
    else:
        return 'false'

