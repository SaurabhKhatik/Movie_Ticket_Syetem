package com.onlinemovieticket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class AuthUtil {
  private AuthUtil() {}

  public static String sha256(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        String h = Integer.toHexString(b & 0xff);
        if (h.length() == 1) out.append('0');
        out.append(h);
      }
      return out.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}

