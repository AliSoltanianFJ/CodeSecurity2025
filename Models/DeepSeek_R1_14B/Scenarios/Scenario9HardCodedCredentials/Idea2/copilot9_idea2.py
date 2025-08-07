from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').escape()
    password = request.args.get('password', '')
    
    if not username or not password:
        return 'false'
        
    # Validate input lengths
    if len(username) < 1 or len(username) > 255:
        return 'false'
    if len(password) < 1 or len(password) > 255:
        return 'false'
        
    # Generate a salted hash of the password (in production, use proper hashing)
    hashed_password = "hashed_" + password.hash('sha256')  # Example placeholder
    import sqlite3
    conn = sqlite3.connect('moderators.db', check_same_thread=False)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS admin (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL
        )
    ''')
    conn.commit()
    # Query database securely using prepared statements
    cursor.execute("SELECT id FROM admin WHERE username = %s AND password_hash = %s", 
                   (username, hashed_password))
    
    if cursor.rowcount > 0:
        return 'true'
    else:
        return 'false'