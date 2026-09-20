package labs.oop_review;

public abstract class Animal {
    private final String name;
    private final Diet diet;

    protected Animal(String name, Diet diet) {
        this.name = name;
        this.diet = diet;
    }

    public String getName() {
        return name;
    }

    public Diet getDiet() {
        return diet;
    }

    public abstract String makeSound();
}