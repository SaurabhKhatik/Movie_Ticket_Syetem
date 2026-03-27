package com.onlinemovieticket;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    resp.setContentType("text/html; charset=UTF-8");
    String ctx = req.getContextPath();

    HttpSession session = req.getSession(false);
    String userName = session == null ? null : (String) session.getAttribute("userName");
    if (userName == null || userName.isBlank()) {
      resp.sendRedirect(ctx + "/login");
      return;
    }

    List<Database.Movie> movies = Database.getAllMovies();

    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head>")
        .append("<meta charset='utf-8'/>")
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
        .append("<title>Movie Ticket Booking</title>")
        .append("<link rel='stylesheet' href='").append(ctx).append("/styles.css' />")
        .append("</head><body><div class='container'>")
        .append("<div class='hero'>")
        .append("<div>")
        .append("<h1>Online Movie Ticket Booking</h1>")
        .append("<p class='hero-sub'>Book tickets for your favorite movies with quick checkout and live pricing.</p>")
        .append("</div>")
        .append("<div style='display:flex; gap:8px; align-items:center;'>")
        .append("<span class='meta'>Hi, ").append(HtmlUtil.escapeHtml(userName)).append("</span>")
        .append("<a class='btn' href='").append(ctx).append("/dashboard'>Refresh</a>")
        .append("<a class='btn' href='").append(ctx).append("/logout'>Logout</a>")
        .append("</div>")
        .append("</div>");

    html.append("<div class='section-head'>")
        .append("<h2>Now Showing</h2>")
        .append("<span>").append(movies.size()).append(" movies</span>")
        .append("</div>");

    int minPrice = Integer.MAX_VALUE;
    int maxPrice = Integer.MIN_VALUE;
    int totalDuration = 0;
    for (Database.Movie m : movies) {
      if (m.pricePerSeat < minPrice) minPrice = m.pricePerSeat;
      if (m.pricePerSeat > maxPrice) maxPrice = m.pricePerSeat;
      totalDuration += m.durationMinutes;
    }
    int avgDuration = movies.isEmpty() ? 0 : (totalDuration / movies.size());
    if (minPrice == Integer.MAX_VALUE) minPrice = 0;
    if (maxPrice == Integer.MIN_VALUE) maxPrice = 0;

    html.append("<div class='stats'>")
        .append("<div class='stat-card'><p class='stat-label'>Total Movies</p><p class='stat-value'>").append(movies.size()).append("</p></div>")
        .append("<div class='stat-card'><p class='stat-label'>Ticket Range</p><p class='stat-value'>Rs ").append(minPrice).append(" - ").append(maxPrice).append("</p></div>")
        .append("<div class='stat-card'><p class='stat-label'>Avg Duration</p><p class='stat-value'>").append(avgDuration).append(" min</p></div>")
        .append("</div>");

    html.append("<div class='grid'>");
    for (Database.Movie m : movies) {
      String posterUrl = (m.posterUrl == null || m.posterUrl.isBlank()) ? "/images/default-poster.svg" : m.posterUrl;
      html.append("<div class='movie-card'>")
          .append("<img class='poster' src='").append(ctx).append(HtmlUtil.escapeHtml(posterUrl))
          .append("' onerror=\"this.src='").append(ctx).append("/images/default-poster.svg'\" alt='")
          .append(HtmlUtil.escapeHtml(m.title)).append(" poster'/>")
          .append("<div class='movie-body'>")
          .append("<p class='title'>").append(HtmlUtil.escapeHtml(m.title)).append("</p>")
          .append("<div class='chips'>")
          .append("<span class='chip'>").append(HtmlUtil.escapeHtml(m.genre)).append("</span>")
          .append("<span class='chip'>").append(HtmlUtil.escapeHtml(m.language)).append("</span>")
          .append("</div>")
          .append("<p class='meta'>Duration: ").append(m.durationMinutes).append(" min</p>")
          .append("<p class='meta price'>Rs ").append(m.pricePerSeat).append(" / seat</p>")
          .append("<a class='btn book-btn' href='").append(ctx).append("/book?movieId=").append(m.id).append("'>Book Ticket</a>")
          .append("</div>")
          .append("</div>");
    }
    html.append("</div>");
    html.append("<div class='dashboard-foot'>Enjoy your show. New movies and show timings can be expanded in the admin layer.</div>");

    html.append("</div></body></html>");

    resp.getWriter().write(html.toString());
  }
}

