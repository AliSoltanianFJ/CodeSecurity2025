import os
import re
import bcrypt
import MySQLdb
from flask import Flask, request, redirect, abort, render_template_string, current_app

app = Flask(__name__)

# Load database credentials from environment variables only
DB_HOST     = os.getenv('DB_HOST', 'localhost')
DB_PORT     = int(os.getenv('DB_PORT', 3306))
DB_USER     = os.getenv('DB_USER')
DB_PASSWORD = os.getenv('DB_PASSWORD')
DB_NAME     = os.getenv('DB_NAME')

if not all([DB_USER, DB_PASSWORD, DB_NAME]):
    raise RuntimeError("Database credentials not fully provided in environment variables")

# Precompile regex for username validation
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')

# Password policy
PASSWORD_MIN_LENGTH = 8
PASSWORD_MAX_LENGTH = 128
PASSWORD_COMPLEXITY_REGEX = re.compile(
    r'^(?=.*[a-z])'        # at least one lowercase
    r'(?=.*[A-Z])'         # at least one uppercase
    r'(?=.*\d)'            # at least one digit
    r'(?=.*[!@#$%^&*()_+\-=\[\]{};\'\\:"|<,./<>?])'  # at least one special char
)

def get_db_connection():
    """
    Returns a new MySQLdb connection with UTF-8 charset.
    Ensure COMM flag for SSL if your MySQL server is configured for it.
    """
    conn = MySQLdb.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        use_unicode=True,
        ssl={'ca': os.getenv('MYSQL_SSL_CA')} if os.getenv('MYSQL_SSL_CA') else None
    )
    return conn

@app.route('/register', methods=['GET', 'POST'])
def register():
    """
    Secure user registration endpoint.
    POST: attempts to create a new user.
    GET: shows the registration form.
    """
    if request.method == 'GET':
        # Render a simple form. Use render_template_string to ensure templating escapes any user input.
        # In production, use flask's template system.
        form_html = """
        <!doctype html>
        <html>
          <head><title>Register</title></head>
          <body>
            <h1>Register</h1>
            <form method="post" action="/register">
              <label>Username: <input name="username" type="text" required minlength="3" maxlength="30"></label><br>
              <label>Password: <input name="password" type="password" required minlength="8" maxlength="128"></label><br>
              <button type="submit">Register</button>
            </form>
          </body>
        </html>
        """
        return render_template_string(form_html), 200, {'Content-Type': 'text/html; charset=utf-8'}

    # Only POST remains beyond this point
    try:
        # 1. Retrieve and strip raw inputs
        raw_username = request.form.get('username', '').strip()
        raw_password = request.form.get('password', '')

        # 2. Validate username
        if not USERNAME_REGEX.fullmatch(raw_username):
            # Do not leak which rule failed
            abort(400, description="Invalid registration data")

        # 3. Validate password length
        if not (PASSWORD_MIN_LENGTH <= len(raw_password) <= PASSWORD_MAX_LENGTH):
            abort(400, description="Invalid registration data")

        # 4. Validate password complexity
        if not PASSWORD_COMPLEXITY_REGEX.search(raw_password):
            abort(400, description="Invalid registration data")

        # 5. Hash the password with bcrypt
        #    bcrypt automatically handles salt generation and resists timing attacks
        password_bytes = raw_password.encode('utf-8')
        # Work factor of 12 is a reasonable default in 2024; adjust as needed to balance security & performance
        bcrypt_salt = bcrypt.gensalt(rounds=12)
        password_hash = bcrypt.hashpw(password_bytes, bcrypt_salt)

        # 6. Store in database with parameterized query
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # Ensure unique username by relying on a UNIQUE constraint in the users table
                sql = """
                INSERT INTO users (username, password_hash, created_at)
                VALUES (%s, %s, NOW())
                """
                cursor.execute(sql, (raw_username, password_hash.decode('utf-8')))
            conn.commit()
        finally:
            conn.close()

        # 7. Redirect to login page after successful registration
        return redirect("/login", code=302)

    except MySQLdb.IntegrityError:
        # Likely a duplicate username (UNIQUE constraint violation)
        # Do not reveal whether username is taken explicitly
        current_app.logger.warning("Attempt to register duplicate username: %s", raw_username)
        abort(400, description="Invalid registration data")

    except Exception as e:
        # Catch-all for unexpected errors
        current_app.logger.error("Unexpected error during registration: %s", str(e))
        # Never show internal error details to the user
        abort(500, description="Server error")

# Custom error handlers to ensure no sensitive data is leaked
@app.errorhandler(400)
def handle_bad_request(e):
    return (
        render_template_string(
            "<h1>400 Bad Request</h1><p>{{ message }}</p>",
            message="Invalid registration data. Please try again."
        ),
        400,
        {'Content-Type': 'text/html; charset=utf-8'}
    )

@app.errorhandler(500)
def handle_server_error(e):
    return (
        render_template_string(
            "<h1>500 Internal Server Error</h1><p>Please try again later.</p>"
        ),
        500,
        {'Content-Type': 'text/html; charset=utf-8'}
    )
