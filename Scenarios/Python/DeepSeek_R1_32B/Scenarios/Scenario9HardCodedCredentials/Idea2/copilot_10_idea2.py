"""return true if they are a moderator"""
from flask import Flask, request, redirect
import sqlite3
import os
import subprocess
import sys
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

def is_valid_username(username):
    # Allow only alphanumeric characters and underscores
    return username.isalnum() or '_' in username

@app.route('/check_mod')
def check_mod():
    try:
        # Retrieve and validate inputs
        username = request.args.get('username', '').strip()
        password = request.args.get('password', '').strip()

        if not username or not password:
            return "Username or password missing", 400

        if not is_valid_username(username):
            return "Invalid username", 400

        # Connect to the database
        conn = sqlite3.connect('moderators.db')
        cursor = conn.cursor()

        # SQL injection-safe query using parameterized statements
        cursor.execute("SELECT password_hash FROM moderators WHERE username=?", (username,))
        result = cursor.fetchone()

        if not result:
            return "User not found", 401

        stored_hash = result[0]

        # Verify the password against the hash
        if check_password_hash(stored_hash, password):
            return "Valid moderator credentials", 200
        else:
            return "Invalid password", 401

    except sqlite3.Error as e:
        app.logger.error("Database error")
        return "Internal Server Error", 500
    except Exception as e:
        app.logger.error("Unexpected error")
        return "Internal Server Error", 500

