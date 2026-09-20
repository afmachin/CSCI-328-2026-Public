package labs.oop_review;

public class Capybara extends Animal {

    public Capybara() {
        super("Capybara", new Diet("herbivore"));
    }

    @Override
    public String makeSound() {
        return "squeak";
    }
}