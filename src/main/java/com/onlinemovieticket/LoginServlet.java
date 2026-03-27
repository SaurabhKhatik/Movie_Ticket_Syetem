package com.onlinemovieticket;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    resp.setContentType("text/html; charset=UTF-8");
    render(resp, req.getContextPath(), null, "", "");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    resp.setContentType("text/html; charset=UTF-8");

    String email = value(req.getParameter("email")).trim();
    String password = value(req.getParameter("password"));
    String ctx = req.getContextPath();

    if (email.isEmpty() || password.isEmpty()) {
      render(resp, ctx, "Please enter email and password.", email, "");
      return;
    }

    Database.User user = Database.findUserByEmailAndPasswordHash(email, AuthUtil.sha256(password));
    if (user == null) {
      render(resp, ctx, "Invalid email or password.", email, "");
      return;
    }

    HttpSession session = req.getSession(true);
    session.setAttribute("userId", user.id);
    session.setAttribute("userName", user.fullName);
    session.setAttribute("userEmail", user.email);

    resp.sendRedirect(ctx + "/dashboard");
  }

  private static void render(HttpServletResponse resp, String ctx, String error, String email, String password) throws IOException {
    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head>")
        .append("<meta charset='utf-8'/>")
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
        .append("<title>Login</title>")
        .append("<link rel='stylesheet' href='").append(ctx).append("/styles.css' />")
        .append("</head><body><div class='login-theme'>")
        .append("<div class='login-card'>")
        .append("<div class='login-brand'>")
        .append("<div class='brand-badge'>")
        .append("<svg width='24' height='24' viewBox='0 0 24 24' fill='none' xmlns='http://www.w3.org/2000/svg'>")
        .append("<path d='M4 5.5C4 4.67 4.67 4 5.5 4H18.5C19.33 4 20 4.67 20 5.5V18.5C20 19.33 19.33 20 18.5 20H5.5C4.67 20 4 19.33 4 18.5V5.5Z' fill='white' fill-opacity='0.16'/>")
        .append("<path d='M10 8L16 12L10 16V8Z' fill='white'/>")
        .append("</svg>")
        .append("</div>")
        .append("<h1 class='brand-title'>Movie Nights</h1>")
        .append("<p class='brand-tagline'>Book your favorite movies instantly</p>")
        .append("</div>")
        .append("<h2 class='auth-title'>Welcome Back</h2>")
        .append("<p class='auth-subtitle'>Login to continue</p>");

    if (error != null && !error.isBlank()) {
      html.append("<div class='error'>").append(HtmlUtil.escapeHtml(error)).append("</div>");
    }

    html.append("<div class='form'><form method='post' action='").append(ctx).append("/login'>")
        .append("<div class='field'><label>Email</label><div class='input-wrap'>")
        .append("<svg class='field-icon' viewBox='0 0 24 24'><path d='M4 6h16a1 1 0 0 1 .8 1.6l-8 6a1 1 0 0 1-1.2 0l-8-6A1 1 0 0 1 4 6zm16 4.25-7.4 5.55a2.95 2.95 0 0 1-3.2 0L4 10.25V18a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7.75z'/></svg>")
        .append("<input type='email' name='email' required value='")
        .append(HtmlUtil.escapeHtml(email))
        .append("'/></div></div>")
        .append("<div class='field'><label>Password</label><div class='input-wrap'>")
        .append("<svg class='field-icon' viewBox='0 0 24 24'><path d='M17 9h-1V7a4 4 0 0 0-8 0v2H7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2zm-6 7.73V18a1 1 0 0 0 2 0v-1.27a2 2 0 1 0-2 0zM10 9V7a2 2 0 1 1 4 0v2h-4z'/></svg>")
        .append("<input type='password' name='password' required value='")
        .append(HtmlUtil.escapeHtml(password))
        .append("'/></div></div>")
        .append("<button class='login-btn' type='submit'>Login</button>")
        .append("<div class='login-links'><a href='#'>Forgot Password?</a><a href='")
        .append(ctx).append("/signup'>Need an account?</a></div>")
        .append("</form></div>")
        .append("<a class='create-account-link' href='").append(ctx).append("/signup'>Create Account</a>")
        .append("</div></div></body></html>");
    resp.getWriter().write(html.toString());
  }

  private static String value(String s) {
    return s == null ? "" : s;
  }
}

