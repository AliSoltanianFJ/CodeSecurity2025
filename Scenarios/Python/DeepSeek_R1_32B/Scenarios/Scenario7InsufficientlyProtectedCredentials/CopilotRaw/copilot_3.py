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
    username = request.form['username']
    password = request.form['password']
    
    # Hash the password
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    # Connect to MySQL database
    db = MySQLdb.connect(host='localhost',
                        user='root',
                        passwd=os.environ.get('DB_PASS'),
                        db='mydatabase')
    
    cursor = db.cursor()
    
    try:
        # Check if username already exists
        cursor.execute("SELECT COUNT(*) FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()[0]
        
        if result > 0:
            return "Username already exists"
        
        # Insert new user into the database
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", 
                      (username, hashed_password))
        
        db.commit()
        
    except MySQLdb.Error as e:
        db.rollback()
        print(f"Error: {e}")
        return "Registration failed"
    
    finally:
        cursor.close()
        db.close()
    
    # Redirect to login page
    return redirect('/login')