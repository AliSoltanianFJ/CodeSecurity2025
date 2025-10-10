from flask import Flask, request, redirect
import re
import bcrypt
import pymysql
from pymysql.cursors import DictCursor
from pymysql.pool import Pool

app = Flask(__name__)

# Database connection pool configuration
db_pool = Pool(
    host='localhost',
    user='your_db_user',
    password='your_db_password',
    db='your_database',
    cursorclass=DictCursor,
    autocommit=True,
    sql_mode=['ANSI_QUOTES', 'STRICT_TRANS_TABLES'],
    charset='utf8mb4'
)

@app.route('/register', methods=['POST'])
def register():
    try:
        # Retrieve form data
        username = request.form.get('username')
        password = request.form.get('password')

        # Input validation and sanitization
        if not username or len(username) < 3 or len(username) > 20:
            return "Invalid username", 400
        if not password or len(password) < 8:
            return "Password must be at least 8 characters long", 400
        
        # Sanitize username (only allow alphanumeric, underscores and hyphens)
        sanitized_username = re.sub(r'[^a-zA-Z0-9_-]', '', username.strip())
        
        # Check if username already exists
        with db_pool.connection() as conn:
            cursor = conn.cursor()
            query = "SELECT COUNT(*) AS count FROM users WHERE username = %s"
            cursor.execute(query, (sanitized_username,))
            result = cursor.fetchone()
            
            if result['count'] > 0:
                return "Username already exists", 409
            
            # Hash the password securely
            hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

            # Insert new user into database
            insert_query = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
            cursor.execute(insert_query, (sanitized_username, hashed_password))
            
        return redirect('/login', code=302)
        
    except pymysql.err.MySQLError as e:
        app.logger.error(f"Database error during registration: {str(e)}")
        return "An error occurred while processing your request", 500
    finally:
        db_pool.close()

# Error handlers and additional security configurations
@app.errorhandler(404)
def page_not_found(error):
    return "Resource not found", 404

@app.errorhandler(500)
def internal_server_error(error):
    return "Internal server error", 500
