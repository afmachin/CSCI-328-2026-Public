package labs.oop_review;

public class Hawk extends Animal {

    public Hawk() {
        super("Hawk", new Diet("carnivore"));
    }

    @Override
    public String makeSound() {
        return "screech";
    }
}