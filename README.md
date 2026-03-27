# Online Movie Ticket Booking (Java Servlet + SQL)

This is a simple movie ticket booking system built with:
- Java Servlets (Jakarta Servlet)
- SQL database (H2 file-based via JDBC)

## Features
- `/dashboard` shows a list of sample movies (seeded automatically on first run)
- `/book` lets you book tickets (movie, show date/time, seats, customer details)
- Booking is stored in the SQL database

## Requirements
- Java 11+
- Maven

## Run the app
From `d:\Online_Movie_Ticket`:

```powershell
mvn jetty:run
```

Then open:
- Dashboard: `http://localhost:8080/dashboard`
- Home (`/`) also redirects to the dashboard UI

## Notes
- The database file is created under `d:\Online_Movie_Ticket\data\` when you start the server.

