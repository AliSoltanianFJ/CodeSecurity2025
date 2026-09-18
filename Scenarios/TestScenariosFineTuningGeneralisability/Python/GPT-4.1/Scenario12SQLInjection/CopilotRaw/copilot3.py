from flask import Flask, request, jsonify
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # Connect to the database (adjust your connection parameters as needed)
    db = MySQLdb.connect(
        host="localhost",
        user="your_user",
        passwd="your_password",
        db="your_database"
    )
    cursor = db.cursor()

    # Use parameterized query to prevent SQL injection
    query = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
    cursor.execute(query, (customer_id, status))
    orders = cursor.fetchall()

    # Convert to a simple output (adjust as needed, e.g., jsonify for API)
    result = ""
    for order in orders:
        result += str(order) + "<br>"

    cursor.close()
    db.close()

    return result if result else "No matching orders found."