package labs.oop_review;

public class Lion extends Animal {

    public Lion() {
        super("Lion", new Diet("carnivore"));
    }

    @Override
    public String makeSound() {
        return "roar";
    }
}