public class Task {
    int id;
    String title;
    String dueDate;
    String priority;
    boolean completed;

    public Task(int id, String title, String dueDate, String priority, boolean completed) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = completed;
    }

    public Object[] toRow() {
        return new Object[]{id, title, dueDate, priority, completed ? "Yes" : "No"};
    }
}