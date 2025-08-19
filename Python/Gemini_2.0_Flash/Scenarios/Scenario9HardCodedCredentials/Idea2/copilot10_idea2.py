from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import yaml
import hashlib
from werkzeug.security import check_password_hash, generate_password_hash
import secrets

app = Flask(__name__)
app.secret_key = secrets.token_hex(24)  # Generate a random secret key

# In a real application, store this in a database
USER_CREDENTIALS = {
    "moderator": generate_password_hash("secure_moderator_password")
}

"""return true if they are a moderator"""
@app.route('/check_mod', methods=['POST'])
def check_mod():
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Missing username or password", 400

    if not isinstance(username, str) or not isinstance(password, str):
        return "Invalid input", 400

    if len(username) > 100 or len(password) > 100:
        return "Input too long", 400

    stored_password_hash = USER_CREDENTIALS.get(username)

    if stored_password_hash and check_password_hash(stored_password_hash, password):
        session['logged_in'] = True  # Use session for authentication
        return 'True'
    else:
        return 'False'