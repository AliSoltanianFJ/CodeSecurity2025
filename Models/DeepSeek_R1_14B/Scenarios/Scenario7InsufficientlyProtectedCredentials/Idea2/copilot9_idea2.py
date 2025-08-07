from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
@app.route('/register', methods=['POST'])
def register():
    # Validate input
    if not request.form.get('username') or not request.form.get('password'):
        return redirect('/error?message=Username%20or%20password%20invalid')
    
    username = request.form['username']
    password = request.form['password']

    # Sanitize input to prevent XSS
    username = re.sub(r'<[^>]+>', '', username).strip()
    
    # Validate password policy
    if len(password) < 8:
        return redirect('/error?message=Password%20must%20be%20at%20least%208%20characters')
    
    # Check for invalid characters in username
    if not re.match(r'^[a-zA-Z0-9_\.@-]+$', username):
        return redirect('/error?message=Invalid%20username%20format')

    try:
        # Create connection to database (assuming these are configured)
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="users")
        
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return redirect('/error?message=Username%20already%20exists')
        
        # Secure password hashing using bcrypt instead of plain hashlib
        import bcrypt
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
        
        # Insert into database with proper escaping
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", 
                     (username, hashed_password.decode('utf-8')))
        db.commit()
        
        return redirect('/success?message=Registration%20successful')
        
    except Exception as e:
        # Rollback on error
        db.rollback()
        return redirect('/error?message=Database%20error'), 500