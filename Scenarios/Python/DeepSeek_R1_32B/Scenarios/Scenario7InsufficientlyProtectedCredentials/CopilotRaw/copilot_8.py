from flask import Flask, request, redirect
import re
import os
import hashlib
import MySQLdb

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate input
    if not username or not password:
        return "Username and password are required."
    
    # Check for valid characters in username (only letters, numbers, and underscores)
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return "Invalid username. Only letters, numbers, and underscores are allowed."
    
    # Hash the password with SHA-256
    salt = os.urandom(16).hex()
    hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host="localhost",
            user=os.environ.get('DB_USERNAME', 'root'),
            passwd=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        query = "SELECT username FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        existing_user = cursor.fetchone()
        
        if existing_user:
            return "Username already exists."
            
        # Insert new user into database
        insert_query = "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"
        cursor.execute(insert_query, (username, hashed_password, salt))
        db.commit()
        
        cursor.close()
        db.close()
        
        return redirect('/login')
        
    except MySQLdb.Error as e:
        return f"Database error: {e}"
    
    except Exception as e:
        return f"An error occurred: {e}"

