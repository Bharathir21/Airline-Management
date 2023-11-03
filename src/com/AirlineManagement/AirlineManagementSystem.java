package com.AirlineManagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class AirlineManagementSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/airline";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "2105";

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // Create tables 
            createTables(connection);

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("1. Add Flight");
                System.out.println("2. Book Seat");
                System.out.println("3. View Flight Details");
                System.out.println("4. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        addFlight(connection, scanner);
                        break;
                    case 2:
                        bookSeat(connection, scanner);
                        break;
                    case 3:
                        viewFlightDetails(connection, scanner);
                        break;
                    case 4:
                        System.out.println("Exiting...");
                        connection.close();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        String createFlightsTableSQL = "CREATE TABLE IF NOT EXISTS flights (id INT AUTO_INCREMENT PRIMARY KEY, flight_number VARCHAR(10), origin VARCHAR(50), destination VARCHAR(50), capacity INT)";
        String createPassengersTableSQL = "CREATE TABLE IF NOT EXISTS passengers (id INT AUTO_INCREMENT PRIMARY KEY, flight_id INT, name VARCHAR(100), seat_number INT)";
        try (PreparedStatement statement = connection.prepareStatement(createFlightsTableSQL)) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(createPassengersTableSQL)) {
            statement.executeUpdate();
        }
    }

    private static void addFlight(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter Flight Number: ");
        String flightNumber = scanner.nextLine();
        System.out.print("Enter Origin: ");
        String origin = scanner.nextLine();
        System.out.print("Enter Destination: ");
        String destination = scanner.nextLine();
        System.out.print("Enter Capacity: ");
        int capacity = scanner.nextInt();

        String insertFlightSQL = "INSERT INTO flights (flight_number, origin, destination, capacity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertFlightSQL)) {
            statement.setString(1, flightNumber);
            statement.setString(2, origin);
            statement.setString(3, destination);
            statement.setInt(4, capacity);
            statement.executeUpdate();
            System.out.println("Flight added successfully!");
        }
    }

    private static void bookSeat(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter Flight Number: ");
        String flightNumber = scanner.nextLine();
        System.out.print("Enter Passenger Name: ");
        String passengerName = scanner.nextLine();

        String checkFlightSQL = "SELECT id, capacity FROM flights WHERE flight_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(checkFlightSQL)) {
            statement.setString(1, flightNumber);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int flightId = resultSet.getInt("id");
                int capacity = resultSet.getInt("capacity");

                String insertPassengerSQL = "INSERT INTO passengers (flight_id, name, seat_number) VALUES (?, ?, ?)";
                try (PreparedStatement insertPassengerStatement = connection.prepareStatement(insertPassengerSQL)) {
                    int seatNumber = bookAvailableSeat(connection, flightId, capacity);
                    if (seatNumber == -1) {
                        System.out.println("No available seats on this flight.");
                    } else {
                        insertPassengerStatement.setInt(1, flightId);
                        insertPassengerStatement.setString(2, passengerName);
                        insertPassengerStatement.setInt(3, seatNumber);
                        insertPassengerStatement.executeUpdate();
                        System.out.println("Seat booked successfully!");
                    }
                }
            } else {
                System.out.println("Flight not found.");
            }
        }
    }

    private static int bookAvailableSeat(Connection connection, int flightId, int capacity) throws SQLException {
        String checkPassengersSQL = "SELECT seat_number FROM passengers WHERE flight_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(checkPassengersSQL)) {
            statement.setInt(1, flightId);
            ResultSet resultSet = statement.executeQuery();
            boolean[] seats = new boolean[capacity];

            while (resultSet.next()) {
                int seatNumber = resultSet.getInt("seat_number");
                if (seatNumber >= 1 && seatNumber <= capacity) {
                    seats[seatNumber - 1] = true;
                }
            }

            for (int i = 0; i < capacity; i++) {
                if (!seats[i]) {
                    return i + 1;
                }
            }
            return -1; // No available seats
        }
    }

    private static void viewFlightDetails(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter Flight Number: ");
        String flightNumber = scanner.nextLine();

        String viewFlightSQL = "SELECT f.flight_number, f.origin, f.destination, f.capacity, p.name, p.seat_number " +
                              "FROM flights f " +
                              "LEFT JOIN passengers p ON f.id = p.flight_id " +
                              "WHERE f.flight_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(viewFlightSQL)) {
            statement.setString(1, flightNumber);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                if (resultSet.isFirst()) {
                    System.out.println("Flight Number: " + resultSet.getString("flight_number"));
                    System.out.println("Origin: " + resultSet.getString("origin"));
                    System.out.println("Destination: " + resultSet.getString("destination"));
                    System.out.println("Capacity: " + resultSet.getInt("capacity"));
                    System.out.println("Passengers:");
                }

                String passengerName = resultSet.getString("name");
                int seatNumber = resultSet.getInt("seat_number");

                if (passengerName != null) {
                    System.out.println("  - " + passengerName + " (Seat " + seatNumber + ")");
                }
            }
        }
    }
}

