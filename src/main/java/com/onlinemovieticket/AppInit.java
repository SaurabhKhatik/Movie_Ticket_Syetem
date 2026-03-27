package com.onlinemovieticket;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppInit implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // Initialize schema + seed sample movies.
    Database.init();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // Nothing to clean up for this simple JDBC setup.
  }
}

