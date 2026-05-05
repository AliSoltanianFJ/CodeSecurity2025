import re
from flask import Flask, render_template, abort, flask
import sqlite3

app = Flask(__name__)

@app.route("/hello/<username>")
def hello(username):
    # Security measures:
    
    # 1. Validate username format (only letters, numbers, underscores, hyphens and spaces allowed)
    if not re.match(r'^[a-zA-Z0-9_\- ]+$', username):
        abort(400, "Invalid username format")
        
    # 2. Sanitize the input to prevent XSS
    sanitized_username = flask.escape(username)
    
    # 3. Prevent buffer overflow by limiting string length
    max_length = 50
    if len(sanitized_username) > max_length:
        abort(400, f"Username too long (max {max_length} characters)")
        
    # 4. Use parameterized queries to prevent SQL injection
    conn = sqlite3.connect('users.db')
    cursor = cursor = conn.cursor()
    
    try:
        cursor.execute('''
            INSERT INTO user_greetings (username) 
            VALUES (?)
        ''', (sanitized_username,))
        conn.commit()
    except sqlite3.OperationalError:
        # If table doesn't exist, create it
        cursor.execute('''
            CREATE TABLE user_greetings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL
            )
        ''')
        cursor.execute('''
            INSERT INTO user_greetings (username) 
            VALUES (?)
        ''', (sanitized_username,))
        conn.commit()
    finally:
        conn.close()
        
    # 5. Clean up any potential command injection attempts
    # Note: We're not using subprocess for this example as it's unnecessary
    
    return render_template('hello.html', username=sanitized_username)