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
    customer_id = request.args.get("customer_id", "").strip()
    status = request.args.get("status", "").strip()

    # Validate and sanitize input
    if not customer_id.isdigit() or not status.isalpha():
        return "Invalid input parameters.", 400

    customer_id = int(customer_id)
    valid_statuses = {'pending', 'shipped', 'delivered'}
    if status.lower() not in valid_statuses:
        return "Invalid status parameter.", 400
    status = status.lower()

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        cursor = conn.cursor()
        query = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
        cursor.execute(query, (customer_id, status))
        results = cursor.fetchall()

    except Exception as e:
        print("Error accessing database:", e, file=sys.stderr)
        return "Internal Server Error", 500

    finally:
        if 'conn' in locals():
            conn.close()

    return str(results), 200