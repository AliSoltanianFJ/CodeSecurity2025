import os
import re
import logging
from flask import Flask, request, redirect, abort
import MySQLdb

app = Flask(__name__)

# Set up basic logging to stderr; in production, route this to a file or a logging service
logging.basicConfig(level=logging.INFO)

# Strict regex for basic email syntax validation
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    """
    Returns a new MySQLdb.Connection using environment variables.
    Expects DB_HOST, DB_USER, DB_PASS, DB_NAME to be set.
    """
    host = os.environ.get('DB_HOST', 'localhost')
    user = os.environ.get('DB_USER')
    passwd = os.environ.get('DB_PASS')
    dbname = os.environ.get('DB_NAME')
    # Ensure all needed credentials are present
    if not all([user, passwd, dbname]):
        logging.error("Database credentials are not fully configured in the environment.")
        abort(500)
    try:
        conn = MySQLdb.connect(
            host=host,
            user=user,
            passwd=passwd,
            db=dbname,
            charset='utf8mb4',
            use_unicode=True
        )
        # Turn off autocommit so we can roll back on errors
        conn.autocommit(False)
        return conn
    except MySQLdb.Error:
        logging.exception("Failed to connect to the database")
        abort(500)

@app.before_request
def enforce_https_in_production():
    """
    Redirect to HTTPS if in production (FLASK_ENV=production) and request is not secure.
    """
    if os.environ.get('FLASK_ENV') == 'production':
        # Flask’s request.is_secure may not detect certain proxy setups;
        # for real deployments you might check X-Forwarded-Proto instead.
        if not request.is_secure:
            url = request.url.replace('http://', 'https://', 1)
            return redirect(url, code=301)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Read and normalize the email parameter
    raw_email = request.args.get('email', '').strip().lower()
    if not raw_email:
        return "Missing email parameter.", 400
    if len(raw_email) > 254 or not EMAIL_REGEX.match(raw_email):
        return "Invalid email address.", 400

    conn = None
    cursor = None
    try:
        # 2) Acquire a database connection
        conn = get_db_connection()
        cursor = conn.cursor()

        # 3) Lock the row for update, if it exists
        select_sql = """
            SELECT 1
              FROM subscribers
             WHERE email = %s
             FOR UPDATE
        """
        cursor.execute(select_sql, (raw_email,))
        row = cursor.fetchone()

        if not row:
            # No such email; no state change
            conn.rollback()
            return "Email address not found.", 404

        # 4) Perform the unsubscribe action (soft delete or flag)
        update_sql = """
            UPDATE subscribers
               SET subscribed = 0, updated_at = NOW()
             WHERE email = %s
        """
        cursor.execute(update_sql, (raw_email,))

        # 5) Commit the transaction
        conn.commit()
        return "You have been unsubscribed successfully.", 200

    except MySQLdb.Error:
        # Log full stack trace server-side but return a generic message
        logging.exception("Database error during unsubscribe for email=%s", raw_email)
        if conn:
            conn.rollback()
        return "Internal server error.", 500

    finally:
        # Clean up
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass
