package registration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Roster {

    private final String crn;
    private final int capacity;
    private final Map<String, Student> students = new HashMap<String, Student>();

    public Roster(String crn, int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        this.crn = crn;
        this.capacity = capacity;
    }

    public String getCrn() { return crn; }

    public int getCapacity() { return capacity; }

    public Map<String, Student> getStudents() {
        return Collections.unmodifiableMap(students);
    }

    public boolean addStudent(Student student) {
        if (students.containsKey(student.getId()) || students.size() >= capacity) {
            return false;
        }
        students.put(student.getId(), student);
        return true;
    }
}