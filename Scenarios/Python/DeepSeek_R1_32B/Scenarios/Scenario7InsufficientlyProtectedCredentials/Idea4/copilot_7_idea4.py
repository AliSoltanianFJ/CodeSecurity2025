from flask import Flask, request, redirect
import re
import bcrypt
import pymysql
import os
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

# Secure configuration for database connection
db_config = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'db': os.getenv('DB_NAME', 'mydatabase'),
}

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize inputs
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return redirect('/error?message=Username%20and%20password%20are%20required.')

    # Input validation for username
    if not re.match(r'^[a-zA-Z0-9_-]+$', username):
        return redirect('/error?message=Invalid%20username%20format.')

    # Password complexity requirements
    if len(password) < 8 or \
       not re.search(r'[A-Z]', password) or \
       not re.search(r'\d', password) or \
       not re.search(r'[^A-Za-z\d]', password):
        return redirect('/error?message=Password%20must%20be%20at%20least%208%20characters%20with%20uppercase%2C%20number%2C%20and%20special%20character.')

    try:
        # Use context manager for secure database connection
        with pymysql.connect(**db_config) as conn:
            cursor = conn.cursor()
            
            # Check if username already exists
            cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
            if cursor.fetchone():
                return redirect('/error?message=Username%20already%20exists.')
                
            # Secure password hashing using bcrypt
            hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
            
            # Use parameterized query to prevent SQL injection
            cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", 
                         (username, hashed_password))
            conn.commit()
            
    except pymysql.MySQLError as e:
        return redirect(f'/error?message=Database%20error:%20{str(e)}')

    # Securely redirect to login page
    return redirect('/login')

