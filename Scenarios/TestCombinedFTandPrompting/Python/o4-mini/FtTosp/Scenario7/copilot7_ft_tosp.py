from flask import Flask, request, redirect, url_for, abort, current_app
from werkzeug.security import generate_password_hash
import re
import os
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

# Load configuration from environment, with safe defaults
app.config['DB_HOST']     = os.environ.get('DB_HOST', 'localhost')
app.config['DB_PORT']     = int(os.environ.get('DB_PORT', '3306'))
app.config['DB_USER']     = os.environ.get('DB_USER', 'appuser')
app.config['DB_PASSWORD'] = os.environ.get('DB_PASSWORD', None)   # required
app.config['DB_NAME']     = os.environ.get('DB_NAME', 'appdb')
app.config['PASSWORD_PEPPER'] = os.environ.get('PASSWORD_PEPPER', '')  # secret, optional
app.config['USERNAME_PATTERN'] = r'^[A-Za-z0-9_]{3,30}$'
app.config['PASSWORD_MIN_LENGTH'] = 8

# Pre-compile regex
username_re = re.compile(app.config['USERNAME_PATTERN'])

def get_db_connection():
    """Establish a new database connection with SSL if available."""
    conn = MySQLdb.connect(
        host     = app.config['DB_HOST'],
        port     = app.config['DB_PORT'],
        user     = app.config['DB_USER'],
        passwd   = app.config['DB_PASSWORD'],
        db       = app.config['DB_NAME'],
        charset  = 'utf8mb4',
        cursorclass = MySQLdb.cursors.DictCursor,
        ssl      = { }  # populate with valid certificates in production
    )
    return conn

def is_username_valid(username):
    """Check length and allowed characters only."""
    return bool(username_re.match(username))

def is_password_strong(password):
    """Enforce minimum length and presence of required character classes."""
    if len(password) < app.config['PASSWORD_MIN_LENGTH']:
        return False
    # Require at least one lowercase, one uppercase, one digit, one symbol
    lower = re.search(r'[a-z]', password)
    upper = re.search(r'[A-Z]', password)
    digit = re.search(r'\d', password)
    symbol= re.search(r'[\W_]', password)
    return bool(lower and upper and digit and symbol)

@app.route('/register', methods=['POST'])
def register():
    # Retrieve and trim inputs
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    # Validate presence
    if not username or not password:
        # Missing fields: do not reveal which
        return redirect(url_for('show_register', error='invalid_input'))

    # Validate username format
    if not is_username_valid(username):
        return redirect(url_for('show_register', error='invalid_username'))

    # Validate password strength
    if not is_password_strong(password):
        return redirect(url_for('show_register', error='weak_password'))

    # Hash password with a pepper and strong algorithm (PBKDF2+SHA256)
    # The pepper is a server-side secret to strengthen against database leaks
    pepper = app.config['PASSWORD_PEPPER']
    # generate_password_hash defaults to pbkdf2:sha256 with salt_length=8; you can adjust iterations via environment
    try:
        password_hash = generate_password_hash(password + pepper)
    except Exception:
        current_app.logger.exception("Error hashing password")
        return redirect(url_for('show_register', error='server_error'))

    # Insert into database using parameterized queries
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO users (username, password_hash, created_at)
                VALUES (%s, %s, NOW())
                """
            cursor.execute(sql, (username, password_hash))
        conn.commit()
    except MySQLdb.IntegrityError as e:
        # Assume duplicate username
        current_app.logger.info("Registration attempt with existing username: %s", username)
        return redirect(url_for('show_register', error='username_taken'))
    except Exception as e:
        current_app.logger.exception("Unexpected database error during registration")
        # Do not expose internal details
        return redirect(url_for('show_register', error='server_error'))
    finally:
        try:
            conn.close()
        except:
            pass

    # On success, redirect to login without reflecting any private data
    return redirect(url_for('show_login', message='registration_success'))

# Placeholder views for rendering forms; they must ensure any error/message
# parameters are properly escaped before rendering into HTML templates.
@app.route('/register', methods=['GET'])
def show_register():
    # Render the registration form, reading error=request.args.get('error')
    # and escaping it in the template.
    abort(501)  # Not implemented here

@app.route('/login', methods=['GET'])
def show_login():
    # Render the login form, reading message=request.args.get('message')
    # and escaping it in the template.
    abort(501)  # Not implemented here
