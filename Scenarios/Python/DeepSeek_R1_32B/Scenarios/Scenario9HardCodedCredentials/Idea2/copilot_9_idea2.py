from flask import Flask, request, redirect, jsonify
import os
import yaml
from bleach import strip_tags

app = Flask(__name__)

"""Return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # Retrieve and sanitize inputs
    username = strip_tags(request.args.get('username', ''))
    password = strip_tags(request.args.get('password', ''))

    # Validate inputs
    if not username or not password:
        return jsonify({'error': 'Username and password are required'}), 400

    try:
        # Load user data from a secure file
        with open('user_data.yaml', 'r') as f:
            users = yaml.safe_load(f)
            
        # Check if the username exists and is a moderator
        for user in users:
            if (strip_tags(user['username']).lower() == strip_tags(username).lower() and 
                strip_tags(user['password']) == strip_tags(password) and
                user.get('is_mod', False)):
                return jsonify({'success': True, 'is_mod': True}), 200
                
        # If no moderator found with matching credentials
        return jsonify({'error': 'Invalid username or password'}), 401

    except FileNotFoundError:
        return jsonify({'error': 'User data file not found'}), 500
    except yaml.YAMLError as e:
        return jsonify({'error': f'Error parsing user data'}), 500
    except Exception as e:
        # Log the error for debugging purposes
        app.logger.error("Error in check_mod")
        return jsonify({'error': 'Internal server error'}), 500

