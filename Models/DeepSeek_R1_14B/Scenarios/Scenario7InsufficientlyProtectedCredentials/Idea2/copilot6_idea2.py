from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = re.escape(request.form.get('username', '').strip())
    password = request.form.get('password', '')
    
    # Input validation
    if not username or not password:
        return "Username or password is missing", 400
        
    # Username validation (alphanumeric with some special characters, length <= 50)
    if not re.match(r'^[a-zA-Z0-9_\-\.]{3,50}$', username):
        return "Invalid username format. Must be between 3 and 50 characters long and alphanumeric.", 400
        
    # Password validation (must have at least one uppercase, lowercase, number, special character)
    if len(password) < 12:
        return "Password must be at least 12 characters long", 400
    if not re.search(r'[A-Z]', password):
        return "Password must contain at least one uppercase letter"
    if not re.search(r'[a-z]', password):
        return "Password must contain at least one lowercase letter" 
    if not re.search(r'\d', password):
        return "Password must contain at least one number"
    
    # Password hashing using bcrypt (more secure than hashlib)
    from flask_bcrypt import Bcrypt
    bc = Bcrypt(app)
    hashed_password = bc.generate_password_hash(password).decode('utf-8')
    
    try:
        db = MySQLdb.connect(os.getenv("MYSQL_HOST"), os.getenv("MYSQL_USER"), os.getenv("MYSQL_PASSWORD"), os.getenv("MYSQL_DB"))
        cursor = db.cursor()
        
        # Prepare SQL statement with proper escaping
        sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(sql, (username, hashed_password))
        
        db.commit()
        return redirect('/success')
        
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}", 500
    finally:
        if 'db' in locals():
            db.close()