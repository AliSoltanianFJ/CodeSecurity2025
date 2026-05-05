from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    # Connect to database
    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'root'),
        passwd=os.environ.get('DB_PASSWORD', ''),
        db=os.environ.get('DB_NAME', 'mydatabase')
    )
    
    cursor = db.cursor()
    try:
        # Check if username already exists
        cursor.execute("SELECT * FROM users WHERE username=%s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists"
        
        # Hash password with SHA-256
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        
        # Insert into database
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", 
                      (username, hashed_password))
        db.commit()
        
    except MySQLdb.Error as e:
        db.rollback()
        return f"Error registering: {e}"
    
    finally:
        cursor.close()
        db.close()
        
    # Redirect to login page
    return redirect(url_for('login'))   