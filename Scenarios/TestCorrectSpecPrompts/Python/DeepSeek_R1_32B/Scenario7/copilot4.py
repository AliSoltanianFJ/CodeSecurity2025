from flask import Flask, request, redirect, url_for
import re
from werkzeug.security import generate_password_hash, check_password_hash
import MySQLdb
import os

app = Flask(__name__)

""" register url """
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate username
    if not username:
        return redirect(url_for('register') + '?error=Username+cannot+be+empty')
    
    # Validate password
    if len(password) < 8:
        return redirect(url_for('register') + '?error=Password+must+be+at+least+8+characters')
    
    # Check for invalid characters in username
    if not re.match(r'^[a-zA-Z0-9_.]+$', username):
        return redirect(url_for('register') + '?error=Invalid+username+characters')
    
    # Hash the password securely
    hashed_password = generate_password_hash(password)
    
    try:
        # Connect to database using environment variables
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return redirect(url_for('register') + '?error=Username+already+exists')
        
        # Insert new user into database
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        
        db.commit()
        cursor.close()
        db.close()
        
        # Redirect to login page upon successful registration
        return redirect(url_for('login'))
        
    except MySQLdb.Error as e:
        # Handle database errors
        return redirect(url_for('register') + f'?error=Database+error+{str(e)}')