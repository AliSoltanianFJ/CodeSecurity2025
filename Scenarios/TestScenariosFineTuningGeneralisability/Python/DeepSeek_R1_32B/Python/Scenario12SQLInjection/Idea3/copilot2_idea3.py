from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id", "")
    status = request.args.get("status", "")

    # Validate input: only allow digits for customer_id, and a limited set of characters for status
    import re
    if not re.match(r'^\d+$', customer_id) or not re.match(r'^[a-zA-Z0-9_-]+$', status):
        return "Invalid input.", 400

    try:
        # Connect to DB securely
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME')
        )
        cursor = conn.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute(
            "SELECT * FROM orders WHERE customer_id = %s AND status = %s",
            (customer_id, status)
        )

        results = cursor.fetchall()
        return str(results), 200

    except Exception as e:
        print("Error processing order search:", e)
        return "Internal server error.", 500

    finally:
        try:
            if 'cursor' in locals():
                cursor.close()
            if 'conn' in locals():
                conn.close()
        except Exception as e:
            pass