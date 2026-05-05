from flask import Flask, request, redirect
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Use a parameterized query to prevent SQL injection
        cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
        result = cursor.fetchone()
        
        if result is None:
            return "Invalid credentials", 401
        
        stored_hash = result[0]
        provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
        
        if provided_hash == stored_hash:
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception as e:
        # Log the exception or handle it as needed
        return "An error occurred during login", 500
    finally:
        cursor.close()
        db.close()

