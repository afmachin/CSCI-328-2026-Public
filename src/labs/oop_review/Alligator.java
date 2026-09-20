package labs.oop_review;

public class Alligator extends Animal {

    public Alligator() {
        super("Alligator", new Diet("carnivore"));
    }

    @Override
    public String makeSound() {
        return "bellow";
    }
}