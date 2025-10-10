from flask import Flask, request, redirect, session
import os
import MySQLdb
import bcrypt

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY')  # Set this in production with a strong secret key!

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')

        # Input validation
        if not username or not password:
            return "Username and password are required"
        
        max_length = 50
        if len(username) > max_length or len(password) > max_length:
            return f"Invalid input. Maximum length is {max_length} characters."
        
        # Database connection using environment variables for credentials
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()
        
        try:
            # Sanitize inputs and prepare SQL query with parameter substitution
            cursor.execute("SELECT password_hash, user_id, role FROM users WHERE username = %s", (username,))
            result = cursor.fetchone()
            
            if not result:
                return "Invalid credentials"
            
            stored_hash, user_id, role = result
            
            # Verify the password against the bcrypt hash
            if not bcrypt.checkpw(password.encode('utf-8'), stored_hash):
                return "Invalid credentials"
            
            # Set session variables
            session['username'] = username
            session['user_id'] = user_id
            session['role'] = role
            
            return redirect('/dashboard')
            
        except MySQLdb.Error as e:
            print(f"Database error: {e}")
            return "Login failed. Please try again."
        finally:
            cursor.close()
            db.close()
    
    # GET request handling (if needed)
    return '''
    <form method="post">
        Username: <input type="text" name="username"><br>
        Password: <input type="password" name="password"><br>
        <input type="submit" value="Login">
    </form>
    '''

