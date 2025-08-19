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
    
    # Validate input
    if not (re.fullmatch(r'^[a-zA-Z0-9_]{3,16}$', username)):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password too short", 400

    # Generate salt
    salt = os.urandom(16).hex()
    
    # Hash password with salt
    hashed_password = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db')
        )
        cursor = db.cursor()
        
        # Insert into database
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt)
        )
        db.commit()
        return redirect('/login')
    except Exception as e:
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        cursor.close()
        db.close()