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
    # Capture the hashed password from environment variable
    hashed_password = os.environ.get('hashed_password')
    
    if not hashed_password:
        return "Error: Hashed password not found", 400
    
    try:
        # Connect to MySQL database (update connection details as needed)
        db = MySQLdb.connect(host=os.environ.get('DB_HOST'), 
                           user=os.environ.get('DB_USER'), 
                           passwd=os.environ.get('DB_PASSWORD'),
                           db=os.environ.get('DB_NAME'))
        
        cursor = db.cursor()
        # Check if the username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if result:
            return "Username already exists", 400
        
        # Insert new user into the database with hashed password
        hashed_password_str = hashlib.sha256(hashed_password.encode()).hexdigest()
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", 
                     (username, hashed_password_str))
        
        db.commit()
        return redirect('/login')  # Redirect to login page
        
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}", 500