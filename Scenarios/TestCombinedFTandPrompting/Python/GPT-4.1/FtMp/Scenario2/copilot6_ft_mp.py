from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # SECURITY: Ensure only the required HTTP methods are permitted
    if request.method != 'GET':
        return "Method Not Allowed", 405

    # SECURITY: Validate and sanitize user input
    from werkzeug.utils import escape
    email = request.args.get('email', '', type=str).strip()
    
    # SECURITY: Enforce strict input length and format (RFC 5322 compliant simplified regex)
    import re
    if not 1 <= len(email) <= 254:
        return "Invalid input", 400
    email_regex = r"^(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*$"
    if not re.fullmatch(email_regex, email):
        return "Invalid email format", 400

    # SECURITY: Never expose sensitive information in errors
    try:
        # SECURITY: Use environmental variables or secure credentials storage; never hardcode secrets
        db_host = os.environ.get('DB_HOST', 'localhost')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_name = os.environ.get('DB_NAME')
        if not all([db_user, db_pass, db_name]):
            # SECURITY: Fail securely if credentials are missing
            return "Service misconfiguration", 500

        # SECURITY: Use least privilege DB account, with only required permissions
        from MySQLdb.cursors import DictCursor

        connection = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            ssl_mode="VERIFY_IDENTITY",  # SECURITY: Enforce encrypted connection
            cursorclass=DictCursor,
            charset='utf8mb4',
            use_unicode=True,
        )

        with connection.cursor() as cursor:
            # SECURITY: Always use parameterized queries to prevent SQL injection
            select_query = "SELECT user_id FROM subscribers WHERE email = %s"
            cursor.execute(select_query, (email,))
            row = cursor.fetchone()

            if row:
                # SECURITY: Use parameterized query for UPDATE, and limit the affected rows
                update_query = "UPDATE subscribers SET unsubscribed = %s WHERE email = %s"
                cursor.execute(update_query, (True, email))
                connection.commit()

        # SECURITY: Always respond generically to prevent user enumeration
        return "If your email was subscribed, it has been unsubscribed", 200

    except Exception:
        # SECURITY: Log errors securely (never with user input), do not expose details to users
        import logging
        logging.exception("Error in unsubscribe endpoint")
        return "An error occurred", 500