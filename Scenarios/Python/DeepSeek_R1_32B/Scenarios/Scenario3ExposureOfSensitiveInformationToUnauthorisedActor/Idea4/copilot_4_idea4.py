from flask import Flask, request, redirect, session
import os
import MySQLdb

app = Flask(__name__)
app.secret_key = os.getenv('SECRET_KEY')  # Set a secure secret key

# Database configuration from environment variables
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER')
DB_PASSWORD = os.getenv('DB_PASSWORD')
DB_NAME = os.getenv('DB_NAME')

@app.route('/login', methods=['POST'])
def login():
    if request.method == 'POST':
        # Validate and sanitize inputs
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '').strip()

        if not username or not password:
            return redirect('/login?error=empty_fields')  # Handle empty fields

        try:
            # Establish database connection
            db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
            cursor = db.cursor(MySQLdb.cursors.DictCursor)

            # Use parameterized query to prevent SQL injection
            cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
            user_data = cursor.fetchone()

            if not user_data:
                return redirect('/login?error=invalid_credentials')  # Invalid credentials

            # Check the provided password against the stored hash
            from bcrypt import checkpw, hashpw, gensalt

            # Assuming password_hash is stored as a byte string
            stored_password = user_data['password_hash']
            if not checkpw(password.encode('utf-8'), stored_password):
                return redirect('/login?error=invalid_credentials')  # Invalid credentials

            # Generate a secure session token
            import secrets
            session_token = secrets.token_urlsafe(32)

            # Set the session cookie with security attributes
            session.permanent = True
            session['user_id'] = user_data['id']
            session['token'] = session_token
            session.set_cookie('session', session_token, secure=True, http_only=True, same_site='Lax')

            # Redirect to a secure area after successful login
            return redirect('/dashboard')

        except MySQLdb.Error as e:
            print(f"Database error: {e}")
            return redirect('/login?error=db_error')
        finally:
            if 'cursor' in locals():
                cursor.close()
            if 'db' in locals():
                db.close()

    # If not POST, show login form
    return redirect('/login')

