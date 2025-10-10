import flask
from flask import Flask, render_template_string, request
import sqlite3
import bcrypt
import re
import os

app = Flask(__name__)
DB_NAME = 'users.db'

# Create database table if it doesn't exist
def init_db():
    conn = None
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS users
                          (id INTEGER PRIMARY KEY AUTOINCREMENT,
                           username TEXT UNIQUE NOT NULL,
                           password_hash TEXT NOT NULL)''')
        conn.commit()
    except Exception as e:
        print(f"Error initializing database: {e}")
    finally:
        if conn:
            conn.close()

# Sanitize and validate username input
def sanitize_username(username):
    # Only allow alphanumeric characters and underscores
    if not re.match('^[a-zA-Z0-9_]+$', username):
        raise ValueError("Invalid username format")
    
    # Limit length to prevent potential attacks
    if len(username) > 255:
        raise ValueError("Username too long")
        
    return flask.escape(username)

@app.route("/hello/<username>")
def hello_user(username):
    try:
        # Validate and sanitize the input
        sanitized_username = sanitize_username(username)
        
        # Securely check if user exists in database
        conn = None
        user_exists = False
        
        try:
            conn = sqlite3.connect(DB_NAME)
            cursor = conn.cursor()
            
            # Use parameterized query to prevent SQL injection
            cursor.execute('SELECT username, password_hash FROM users WHERE username = ?', (username,))
            result = cursor.fetchone()
            
            if result:
                user_exists = True
                
        except sqlite3.Error as e:
            print(f"Database error: {e}")
            return "Internal Server Error", 500
        finally:
            if conn:
                conn.close()
        
        # Get greeting message from environment variable
        GREETING_MESSAGE = os.getenv('GREETING_MESSAGE', "Hello")
        
        if user_exists:
            # Securely render template with escaped data
            return render_template_string(
                f"{GREETING_MESSAGE}, {{username}}!", 
                username=sanitized_username
            )
        else:
            return f"User {sanitized_username} not found", 404
            
    except ValueError as e:
        # Handle invalid input gracefully
        app.logger.warning(f"Invalid input: {str(e)}")
        return "Invalid username format", 400
        
    except Exception as e:
        app.logger.error(f"Unexpected error: {str(e)}")
        return "Internal Server Error", 500