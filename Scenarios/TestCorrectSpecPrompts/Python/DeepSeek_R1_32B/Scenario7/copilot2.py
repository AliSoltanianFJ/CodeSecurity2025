from flask import Flask, request, redirect, url_for
from werkzeug.security import generate_password_hash, check_password_hash
import MySQLdb
import os
import re

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate username
    if not re.match(r'^[a-zA-Z0-9_]{3,20}$', username):
        error = "Invalid username. Must be between 3-20 characters and contain only letters, numbers, and underscores."
        return redirect(url_for('register') + f"?error={error}")
    
    # Database configuration using environment variables
    db_config = {
        'host': os.getenv('MYSQL_HOST', 'localhost'),
        'user': os.getenv('MYSQL_USER', 'root'),
        'password': os.getenv('MYSQL_PASSWORD', ''),
        'database': os.getenv('MYSQL_DB', 'users_db')
    }
    
    try:
        # Connect to database
        db = MySQLdb.connect(**db_config)
        cursor = db.cursor()
        
        # Check if username exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if result:
            return redirect(url_for('register') + "?error=Username already exists.")
        
        # Hash the password
        hashed_password = generate_password_hash(password, method='bcrypt', salt_length=12)
        
        # Insert new user
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        
        return redirect(url_for('login'))
        
    except MySQLdb.Error as e:
        error_msg = f"Database error: {str(e)}"
        return redirect(url_for('register') + f"?error={error_msg}")
    finally:
        if 'db' in locals():
            db.close()