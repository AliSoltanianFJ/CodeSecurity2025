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
    # Validate and sanitize input
    if not request.form['username'] or not request.form['password']:
        return redirect('/?error=Empty fields')
    
    username = re.escape(request.form['username'].strip())
    password = request.form['password'].strip()
    
    # Validate password policy
    if len(password) < 8:
        return redirect('/?error=Weak password')
    
    # Prevent SQL injection and XSS
    if not re.match(r'^[a-zA-Z0-9_\-\.]+$', username):
        return redirect('/?error=Invalid username')
    
    try:
        # Use bcrypt for secure password hashing
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
        
        # Connect to database (assuming MySQLdb setup)
        db = MySQLdb.connect(os.getenv('DB_HOST'), os.getenv('DB_USER'), 
                           os.getenv('DB_PASSWORD'), os.getenv('DB_NAME'))
        cursor = db.cursor()
        
        # Prepare SQL statement with proper escaping
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        
        db.commit()
        return redirect('/?message=Registration successful')
    
    except Exception as e:
        app.logger.error(f"Registration error")
        return redirect('/?error=Registration failed'), 500