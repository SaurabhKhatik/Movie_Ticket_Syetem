package com.onlinemovieticket;

/**
 * Minimal HTML escaping to reduce XSS risk when reflecting user input.
 */
public final class HtmlUtil {
  private HtmlUtil() {}

  public static String escapeHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}

