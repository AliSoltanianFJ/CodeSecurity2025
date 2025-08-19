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
    
    # Generate salt
    salt = os.urandom(16).hex()
    
    # Create HMAC for password
    secret_key = os.getenv('SECRET_KEY', 'your-secret-key')
    hmac = hashlib.sha256((password + secret_key).encode()).hexdigest()
    
    # Store in database
    db = MySQLdb.connect(...)
    cursor = db.cursor()
    
    try:
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)", 
                     (username, hmac, salt))
        db.commit()
        return redirect('/login')
    except Exception as e:
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        cursor.close()
        db.close()