package com.onlinemovieticket;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet(urlPatterns = {"/book"})
public class BookingServlet extends HttpServlet {
  private static final String[] SHOW_TIMES = {"10:00", "13:00", "16:00", "19:30"};
  private static final int MIN_SEATS = 1;
  private static final int MAX_SEATS = 8;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    resp.setContentType("text/html; charset=UTF-8");
    String ctx = req.getContextPath();

    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("userId") == null) {
      resp.sendRedirect(ctx + "/login");
      return;
    }

    String movieIdStr = req.getParameter("movieId");
    Integer movieId = tryParseInt(movieIdStr);

    if (movieId == null) {
      writeErrorPage(resp, "Missing or invalid `movieId` parameter.", ctx + "/dashboard", ctx);
      return;
    }

    Database.Movie movie = Database.getMovieById(movieId);
    if (movie == null) {
      writeErrorPage(resp, "Movie not found (id=" + movieId + ").", ctx + "/dashboard", ctx);
      return;
    }

    LocalDate showDate = LocalDate.now();
    String userName = (String) session.getAttribute("userName");
    String userEmail = (String) session.getAttribute("userEmail");
    renderBookingForm(resp, movie, null, showDate, SHOW_TIMES[0], 1, safe(userName), safe(userEmail), "", ctx);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    resp.setContentType("text/html; charset=UTF-8");
    String ctx = req.getContextPath();

    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("userId") == null) {
      resp.sendRedirect(ctx + "/login");
      return;
    }

    String movieIdStr = req.getParameter("movieId");
    Integer movieId = tryParseInt(movieIdStr);
    if (movieId == null) {
      writeErrorPage(resp, "Missing or invalid `movieId` parameter.", ctx + "/dashboard", ctx);
      return;
    }

    Database.Movie movie = Database.getMovieById(movieId);
    if (movie == null) {
      writeErrorPage(resp, "Movie not found (id=" + movieId + ").", ctx + "/dashboard", ctx);
      return;
    }

    String showDateStr = req.getParameter("showDate");
    String showTime = safe(req.getParameter("showTime"));
    String seatsStr = req.getParameter("seats");
    Integer seats = tryParseInt(seatsStr);

    String customerName = safe(req.getParameter("customerName"));
    String customerEmail = safe(req.getParameter("customerEmail"));
    String customerPhone = safe(req.getParameter("customerPhone"));

    String error = null;

    LocalDate showDate;
    try {
      showDate = LocalDate.parse(showDateStr);
    } catch (DateTimeParseException | NullPointerException e) {
      error = "Please choose a valid show date.";
      showDate = LocalDate.now();
    }

    if (error == null) {
      if (showTime == null || showTime.isBlank()) {
        error = "Please choose a show time.";
      } else if (!isAllowedShowTime(showTime)) {
        error = "Invalid show time.";
      }
    }

    if (error == null) {
      if (seats == null || seats < MIN_SEATS || seats > MAX_SEATS) {
        error = "Please select seats between " + MIN_SEATS + " and " + MAX_SEATS + ".";
      }
    }

    if (error == null) {
      if (customerName.trim().length() < 2) {
        error = "Please enter your name.";
      } else if (customerEmail.trim().isEmpty() || !customerEmail.contains("@")) {
        error = "Please enter a valid email.";
      }
    }

    if (error != null) {
      renderBookingForm(resp, movie, error, showDate, showTime, seats == null ? 1 : seats, customerName,
          customerEmail, customerPhone, ctx);
      return;
    }

    int totalPrice = seats * movie.pricePerSeat;
    int bookingId = Database.createBooking(
        movie.id, showDate, showTime, seats, customerName, customerEmail, customerPhone, totalPrice);

    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head>")
        .append("<meta charset='utf-8'/>")
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
        .append("<title>Booking Confirmed</title>")
        .append("<link rel='stylesheet' href='").append(ctx).append("/styles.css' />")
        .append("</head><body><div class='container'>")
        .append("<div class='header'>")
        .append("<h1>Booking Confirmed</h1>")
        .append("<div><a class='btn' href='").append(ctx).append("/dashboard'>Back to Dashboard</a></div>")
        .append("</div>");

    html.append("<div class='success'>Your booking is confirmed.</div>");
    html.append("<div class='form'>")
        .append("<div style='margin-bottom:10px; font-weight:700;'>Booking ID: ")
        .append(bookingId)
        .append("</div>")
        .append("<div class='meta'>Movie: ").append(HtmlUtil.escapeHtml(movie.title)).append("</div>")
        .append("<div class='meta'>Show: ").append(showDate).append(" at ").append(HtmlUtil.escapeHtml(showTime))
        .append("</div>")
        .append("<div class='meta'>Seats: ").append(seats).append("</div>")
        .append("<div class='meta' style='margin-top:8px;'>Total Price: Rs ").append(totalPrice).append("</div>")
        .append("</div>");

    html.append("</div></body></html>");

    resp.getWriter().write(html.toString());
  }

  private static void renderBookingForm(HttpServletResponse resp, Database.Movie movie, String error,
      LocalDate showDate, String showTime, int seats, String customerName, String customerEmail, String customerPhone,
      String ctx)
      throws IOException {
    String showDateValue = showDate == null ? LocalDate.now().toString() : showDate.toString();

    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head>")
        .append("<meta charset='utf-8'/>")
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
        .append("<title>Book Ticket</title>")
        .append("<link rel='stylesheet' href='").append(ctx).append("/styles.css' />")
        .append("</head><body><div class='container'>")
        .append("<div class='header'>")
        .append("<h1>Book Ticket</h1>")
        .append("<div><a class='btn' href='").append(ctx).append("/dashboard'>Dashboard</a></div>")
        .append("</div>");

    if (error != null && !error.isBlank()) {
      html.append("<div class='error'>").append(HtmlUtil.escapeHtml(error)).append("</div>");
    }

    html.append("<div class='form'>")
        .append("<div class='meta' style='margin-top:0; font-weight:700;'>")
        .append(HtmlUtil.escapeHtml(movie.title))
        .append("</div>")
        .append("<div class='meta'>Price: Rs ").append(movie.pricePerSeat).append(" / seat</div>")
        .append("<hr style='border:0; border-top:1px solid rgba(255,255,255,0.12); margin:14px 0;'/>")
        .append("<form method='post' action='").append(ctx).append("/book'>")
        .append("<input type='hidden' name='movieId' value='").append(movie.id).append("'/>")

        .append("<div class='field'>")
        .append("<label>Show Date</label>")
        .append("<input type='date' name='showDate' value='").append(HtmlUtil.escapeHtml(showDateValue)).append("' required/>")
        .append("</div>")

        .append("<div class='field'>")
        .append("<label>Show Time</label>")
        .append("<select name='showTime' required>");
    for (String t : SHOW_TIMES) {
      html.append("<option value='").append(t).append("'");
      if (t.equals(showTime)) html.append(" selected");
      html.append(">").append(t).append("</option>");
    }
    html.append("</select>")
        .append("</div>")

        .append("<div class='field'>")
        .append("<label>Seats (1 - 8)</label>")
        .append("<input type='number' name='seats' min='").append(MIN_SEATS).append("' max='").append(MAX_SEATS).append("' value='")
        .append(seats).append("' required/>")
        .append("</div>")

        .append("<div class='field'>")
        .append("<label>Your Name</label>")
        .append("<input type='text' name='customerName' value='").append(HtmlUtil.escapeHtml(customerName))
        .append("' placeholder='e.g. Rahul' required/>")
        .append("</div>")

        .append("<div class='field'>")
        .append("<label>Your Email</label>")
        .append("<input type='email' name='customerEmail' value='").append(HtmlUtil.escapeHtml(customerEmail))
        .append("' placeholder='e.g. rahul@email.com' required/>")
        .append("</div>")

        .append("<div class='field'>")
        .append("<label>Your Phone</label>")
        .append("<input type='text' name='customerPhone' value='").append(HtmlUtil.escapeHtml(customerPhone))
        .append("' placeholder='e.g. 9876543210'/>")
        .append("</div>")

        .append("<button class='btn' type='submit' style='width:100%; text-align:center;'>Confirm Booking</button>")
        .append("</form>")
        .append("</div>");

    html.append("</div></body></html>");
    resp.getWriter().write(html.toString());
  }

  private static void writeErrorPage(HttpServletResponse resp, String message, String backUrl, String ctx) throws IOException {
    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head>")
        .append("<meta charset='utf-8'/>")
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
        .append("<title>Error</title>")
        .append("<link rel='stylesheet' href='").append(ctx).append("/styles.css' />")
        .append("</head><body><div class='container'>")
        .append("<div class='header'>")
        .append("<h1>Something went wrong</h1>")
        .append("<div><a class='btn' href='").append(backUrl).append("'>Go Back</a></div>")
        .append("</div>")
        .append("<div class='error'>").append(HtmlUtil.escapeHtml(message)).append("</div>")
        .append("</div></body></html>");
    resp.getWriter().write(html.toString());
  }

  private static Integer tryParseInt(String s) {
    if (s == null) return null;
    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean isAllowedShowTime(String showTime) {
    for (String t : SHOW_TIMES) {
      if (t.equals(showTime)) return true;
    }
    return false;
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}

