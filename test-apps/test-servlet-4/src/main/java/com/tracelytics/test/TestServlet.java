package com.tracelytics.test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;
import java.io.IOException;

@WebServlet("")
public class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PushBuilder pushBuilder = request.newPushBuilder();
        if (pushBuilder == null) {
            getServletContext().getRequestDispatcher("/error.jsp").forward(request, response);
        } else {
            pushBuilder.path("resources/images/lanlan.gif").push();
            getServletContext().getRequestDispatcher("/success.jsp").forward(request, response);
        }



    }
}
