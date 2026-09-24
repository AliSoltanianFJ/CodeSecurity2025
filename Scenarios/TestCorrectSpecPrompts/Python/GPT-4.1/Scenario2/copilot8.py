from flask import Flask, request, redirect, render_template_string
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    try:
        email = request.args.get('email')
        if not email:
            return "Email required.", 400

        # Pull credentials from environment variables - not hardcoded
        db_host = os.environ.get('DB_HOST')
        db_name = os.environ.get('DB_NAME')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')

        # Basic check for missing credentials
        if not all([db_host, db_name, db_user, db_pass]):
            return "Server misconfiguration.", 500
        
        conn = MySQLdb.connect(host=db_host, db=db_name, user=db_user, passwd=db_pass)
        cur = conn.cursor()
        
        # Parameterized query (prevents SQL injection)
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        
        if not user:
            return "Email not found.", 404
        
        # Unsubscribe the user (e.g., setting unsubscribed flag)
        cur.execute("UPDATE users SET unsubscribed = 1 WHERE email = %s", (email,))
        conn.commit()
        
        return "You have been unsubscribed."
        
    except Exception as e:
        # Log the real error on the server side only
        app.logger.error("Unsubscribe error: %s", repr(e))
        return "An error occurred.", 500

    finally:
        try:
            cur.close()
            conn.close()
        except:
            pass

# Example usage (set environment variables before running):
# export DB_HOST=localhost
# export DB_NAME=mydb
# export DB_USER=myuser
# export DB_PASS=mypassword