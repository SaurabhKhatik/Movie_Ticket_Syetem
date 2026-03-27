package com.onlinemovieticket;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/signup"})
public class SignupServlet extends HttpServlet {
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

    String fullName = value(req.getParameter("fullName")).trim();
    String email = value(req.getParameter("email")).trim();
    String password = value(req.getParameter("password"));
    String ctx = req.getContextPath();

    if (fullName.length() < 2) {
      render(resp, ctx, "Please enter your full name.", fullName, email);
      return;
    }
    if (email.isEmpty() || !email.contains("@")) {
      render(resp, ctx, "Please enter a valid email.", fullName, email);
      return;
    }
    if (password.length() < 6) {
      render(resp, ctx, "Password must be at least 6 characters.", fullName, email);
      return;
    }
    if (Database.userExistsByEmail(email)) {
      render(resp, ctx, "An account with this email already exists.", fullName, email);
      return;
    }

    int userId = Database.createUser(fullName, email, AuthUtil.sha256(password));
    HttpSession session = req.getSession(true);
    session.setAttribute("userId", userId);
    session.setAttribute("userName", fullName);
    session.setAttribute("userEmail", email);
    resp.sendRedirect(ctx + "/dashboard");
  }

  private static void render(HttpServletResponse resp, String ctx, String error, String fullName, String email) throws IOException {
    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head>")
        .append("<meta charset='utf-8'/>")
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
        .append("<title>Sign Up</title>")
        .append("<link rel='stylesheet' href='").append(ctx).append("/styles.css' />")
        .append("</head><body><div class='container auth-wrap'>")
        .append("<div class='auth-shell'>")
        .append("<div class='auth-side'>")
        .append("<h1>Create Your Account</h1>")
        .append("<p>Join the platform to explore the latest movies and reserve seats instantly.</p>")
        .append("<p>Once registered, you can book directly from the upgraded dashboard.</p>")
        .append("</div>")
        .append("<div class='auth-card'>")
        .append("<h2 class='auth-title'>Sign Up</h2>")
        .append("<p class='auth-subtitle'>Setup your profile in less than a minute</p>");

    if (error != null && !error.isBlank()) {
      html.append("<div class='error'>").append(HtmlUtil.escapeHtml(error)).append("</div>");
    }

    html.append("<div class='form'><form method='post' action='").append(ctx).append("/signup'>")
        .append("<div class='field'><label>Full Name</label><input type='text' name='fullName' required value='")
        .append(HtmlUtil.escapeHtml(fullName))
        .append("'/></div>")
        .append("<div class='field'><label>Email</label><input type='email' name='email' required value='")
        .append(HtmlUtil.escapeHtml(email))
        .append("'/></div>")
        .append("<div class='field'><label>Password</label><input type='password' name='password' required/></div>")
        .append("<button class='btn' type='submit' style='width:100%;'>Create Account</button>")
        .append("</form></div>")
        .append("<p class='auth-switch'>Already have account? <a class='btn' href='").append(ctx).append("/login'>Login</a></p>")
        .append("</div></div></div></body></html>");
    resp.getWriter().write(html.toString());
  }

  private static String value(String s) {
    return s == null ? "" : s;
  }
}

