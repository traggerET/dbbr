package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {



        String[] cmd = {"/bin/bash","-c","echo 31313541 | sudo -S /home/tihon/IdeaProjects/DBBreaker/src/main/resources/corruptFile.sh /home/tihon/IdeaProjects/DBBreaker/src/test/t"};
        Process pb = null;
        try {
            pb = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(pb.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = input.readLine()) != null) {
                sb.append(line);
            }
            input.close();
        } catch (IOException e) {

        }
//        String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
//        String username = "tihon";
//        String password = "31313541";
//
//
//        // Register the PostgreSQL driver
//
//        try {
//            Class.forName("org.postgresql.Driver");
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Connect to the database
//
//        Connection connection = null;
//        try {
//            connection = DriverManager.getConnection(jdbcUrl, username, password);
//            Statement statement = connection.createStatement();
//            ResultSet resultSet = statement.executeQuery("select * from actor;");
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }

        // Perform desired database operations

        // Close the connection
//        try {
//            connection.close();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
    }
}