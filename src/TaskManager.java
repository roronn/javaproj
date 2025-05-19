import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaskManager extends JFrame {
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField titleField;
    private JDateChooser dateChooser; // Use JDateChooser
    private JComboBox<String> priorityBox;

    String csvFilePath = "data/testdata.csv";

    public TaskManager() {
        setTitle("Task Manager");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top input panel
        JPanel inputPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        titleField = new JTextField();
        dateChooser = new JDateChooser(); // Initialize JDateChooser
        priorityBox = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        JButton addButton = new JButton("Add Task");

        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Due Date:"));
        inputPanel.add(dateChooser); // Add JDateChooser
        inputPanel.add(new JLabel(""));
        inputPanel.add(addButton);
        add(inputPanel, BorderLayout.NORTH);


        // Table setup
        String[] columns = {"ID", "Title", "Due Date", "Priority", "Completed"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only the "Priority" column is editable
            }
        };
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Set up the JComboBox as the editor for the "Priority" column
        TableColumn priorityColumn = table.getColumnModel().getColumn(3);
        priorityColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(new String[]{"High", "Medium", "Low"})));

        // Add a TableModelListener to save changes when the priority is edited
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 3) {
                int row = e.getFirstRow();
                String newPriority = (String) tableModel.getValueAt(row, 3);
                try {
                    int taskId = Integer.parseInt((String) tableModel.getValueAt(row, 0));
                    updateTaskPriorityInCsv(taskId, newPriority);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid Task ID.", "Error", JOptionPane.ERROR_MESSAGE);
                    loadTasks(); // Reload to revert potential invalid edit
                }
            }
        });


        // Bottom panel
        JPanel buttonPanel = new JPanel();
        JButton completeButton = new JButton("Mark Complete");
        JButton deleteButton = new JButton("Delete Task");
        buttonPanel.add(completeButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load tasks from CSV
        loadTasks();
        sortTasksByPriority(); // Initial sort when tasks are loaded

        // Add Task
        addButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            Date selectedDate = dateChooser.getDate(); // Get selected date
            String priority = (String) priorityBox.getSelectedItem();

            if (title.isEmpty() || selectedDate == null) { // Check if date is selected
                JOptionPane.showMessageDialog(this, "Title and Due Date are required.");
                return;
            }

            // Format the date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String date = sdf.format(selectedDate);

            try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath, true))) { // Append to the file
                String[] newTaskData = new String[]{
                        String.valueOf(getNextId()), // Generate a new ID
                        title,
                        date,
                        priority,
                        String.valueOf(false)
                };
                writer.writeNext(newTaskData);
                System.out.println("Task added to CSV file: " + csvFilePath);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            clearFields();
            loadTasks();
            sortTasksByPriority(); // Sort after adding
        });

        // Mark Complete
        completeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                try {
                    int idToComplete = Integer.parseInt((String) tableModel.getValueAt(row, 0));
                    markTaskComplete(idToComplete);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid Task ID.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a task to mark as complete.", "Information", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Delete Task
        deleteButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                try {
                    int idToDelete = Integer.parseInt((String) tableModel.getValueAt(row, 0));
                    deleteTask(idToDelete);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid Task ID.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a task to delete.", "Information", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        setVisible(true);
    }


    private int getNextId() {
        int maxId = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int id = Integer.parseInt((String) tableModel.getValueAt(i, 0));
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException e) {
                // Handle potential non-integer ID
            }
        }
        return maxId + 1;
    }

    private void markTaskComplete(int idToComplete) {
        List<String[]> allRows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0 && nextLine[0].equals(String.valueOf(idToComplete))) {
                    nextLine[4] = "true"; // Mark as complete
                }
                allRows.add(nextLine);
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath, false))) { // Overwrite the file
            writer.writeAll(allRows);
            JOptionPane.showMessageDialog(this, "Task marked as complete.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error writing to CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        loadTasks();
        sortTasksByPriority(); // Sort after marking as complete
    }

    private void deleteTask(int idToDelete) {
        List<String[]> allRows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0 && !nextLine[0].equals(String.valueOf(idToDelete))) {
                    allRows.add(nextLine);
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath, false))) { // Overwrite the file
            writer.writeAll(allRows);
            JOptionPane.showMessageDialog(this, "Task deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error writing to CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        loadTasks();
        sortTasksByPriority(); // Sort after deleting
    }

    private void loadTasks() {
        tableModel.setRowCount(0);
        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length == 5) {
                    tableModel.addRow(nextLine);
                } else {
                    System.err.println("Skipping invalid row in CSV: " + String.join(",", nextLine));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
            System.err.println("CSV validation error occurred: " + e.getMessage());
        }
        sortTasksByPriority(); // Sort after loading
    }

    private void clearFields() {
        titleField.setText("");
        dateChooser.setDate(null); // Clear the date in JDateChooser
        priorityBox.setSelectedIndex(0);
    }

    private void updateTaskPriorityInCsv(int taskId, String newPriority) {
        List<String[]> allRows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0 && nextLine[0].equals(String.valueOf(taskId))) {
                    nextLine[3] = newPriority; // Update the priority
                }
                allRows.add(nextLine);
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath, false))) {
            writer.writeAll(allRows);
            System.out.println("Task ID " + taskId + " priority updated to " + newPriority);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error writing to CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        sortTasksByPriority(); // Sort after updating priority
    }

    private void sortTasksByPriority() {
        java.util.List<String[]> data = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String[] rowData = new String[tableModel.getColumnCount()];
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                rowData[j] = (String) tableModel.getValueAt(i, j);
            }
            data.add(rowData);
        }

        java.util.Comparator<String[]> priorityComparator = (task1, task2) -> {
            String priority1 = task1[3]; // Priority is in the 4th column (index 3)
            String priority2 = task2[3];

            // Define the order of priority: High > Medium > Low
            int order1 = getPriorityOrder(priority1);
            int order2 = getPriorityOrder(priority2);

            return Integer.compare(order2, order1); // Sort in descending order (High first)
        };

        data.sort(priorityComparator);

        // Clear the table and add the sorted data
        tableModel.setRowCount(0);
        for (String[] row : data) {
            tableModel.addRow(row);
        }
    }

    private int getPriorityOrder(String priority) {
        return switch (priority.toLowerCase()) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0; // Default to lowest priority if unknown
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TaskManager::new);
    }
}