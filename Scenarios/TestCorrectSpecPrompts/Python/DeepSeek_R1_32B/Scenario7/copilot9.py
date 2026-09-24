from flask import Flask, request, redirect
import re
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Validate username
    username = request.form['username']
    if not re.match(r'^[a-zA-Z0-9_\-]+$', username):
        return 'Invalid username'
    
    # Hash password securely
    password = request.form['password']
    hashed_password = generate_password_hash(password)
    
    # Connect to MySQL database
    db = MySQLdb.connect(
        host=os.getenv('MYSQL_HOST'),
        user=os.getenv('MYSQL_USER'),
        passwd=os.getenv('MYSQL_PASSWORD'),
        db=os.getenv('MYSQL_DB')
    )
    
    try:
        cursor = db.cursor()
        
        # Check if username already exists
        sql_check = "SELECT id FROM users WHERE username = %s"
        cursor.execute(sql_check, (username,))
        result = cursor.fetchone()
        
        if result:
            return 'Username already exists'
        
        # Insert new user
        sql_insert = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(sql_insert, (username, hashed_password))
        
        db.commit()
        cursor.close()
        db.close()
        
        return redirect('/success')
        
    except MySQLdb.Error as e:
        db.rollback()
        print(f"Database error: {e}")  # Log the error
        return 'Registration failed'