from flask import Flask, request, redirect, url_for, session
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = 'your_secret_key'  # Replace with a secure secret key

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Check if the username exists
        query = "SELECT hashed_password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Invalid username or password"
        
        stored_hashed_password = result[0]
        
        # Hash the input password
        salt = 'your_salt'  # Replace with your actual salt
        hashed_input = hashlib.sha256((password + salt).encode()).hexdigest()
        
        if hashed_input == stored_hashed_password:
            session['username'] = username
            return redirect(url_for('dashboard'))
        else:
            return "Invalid username or password"
    except MySQLdb.Error as e:
        return f"Database error: {e}"
    finally:
        db.close()

