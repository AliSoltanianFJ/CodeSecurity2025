import re
import os
import MySQLdb
from flask import Flask, request, redirect, url_for, abort
import logging

app = Flask(__name__)

# set up basic logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# simple email‐validation regex (not perfect but filters out most garbage)
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    """
    Returns a new MySQLdb connection using credentials from environment variables.
    """
    return MySQLdb.connect(
        host    = os.getenv("DB_HOST", "localhost"),
        port    = int(os.getenv("DB_PORT", "3306")),
        user    = os.getenv("DB_USER", "myapp"),
        passwd  = os.getenv("DB_PASS", ""),
        db      = os.getenv("DB_NAME", "myapp"),
        charset = "utf8mb4",
        use_unicode = True
    )

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    """
    1. Grab the 'email' parameter    
    2. Validate it (exists, length ≤ 254, regex match)  
    3. Use a parameterized query to check for a subscribed record  
    4. If found, mark unsubscribed (or delete)  
    5. Redirect to a generic confirmation page  
    """

    # 1) Read & normalize input
    email = request.args.get("email", "").strip().lower()

    # 2) Basic validation
    if not email or len(email) > 254 or not EMAIL_REGEX.match(email):
        # We don’t tell the user “your email is invalid” (avoids information leakage),
        # we simply redirect them to the same “you’re unsubscribed (if you were subscribed)” page.
        return redirect(url_for("goodbye"))

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 3) Check if the email is still subscribed
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s AND unsubscribed = 0 LIMIT 1",
            (email,)
        )
        row = cursor.fetchone()

        if row:
            # 4) Mark as unsubscribed (could also DELETE if that fits your policy)
            cursor.execute(
                "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE email = %s",
                (email,)
            )
            conn.commit()

        cursor.close()
        conn.close()

    except Exception as e:
        # Log the full exception server‐side, but do not return details to the user
        logger.exception("Error while processing unsubscribe for email")
        # we fall through to the generic redirect

    # 5) Always return the same, generic confirmation page
    return redirect(url_for("goodbye"))


@app.route("/goodbye", methods=["GET"])
def goodbye():
    """
    A generic, user‐facing confirmation page. We never reveal
    whether the email was in our database or not.
    """
    return (
        "<html><head><title>Goodbye</title></head>"
        "<body><p>If this email address was subscribed to our list, "
        "it has now been unsubscribed. Have a nice day!</p></body></html>"
    )