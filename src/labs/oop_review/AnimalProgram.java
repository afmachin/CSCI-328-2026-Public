package labs.oop_review;

import java.util.Scanner;

public class AnimalProgram {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Animal[] animals = {
            new Capybara(),
            new Lion(),
            new Alligator(),
            new Hawk()
        };

        System.out.println("Welcome to the Animal Information Program!");
        System.out.println("Available animals: capybara, lion, alligator, and hawk");
        System.out.print("Enter an animal name: ");

        String animalName = scanner.nextLine().trim();
        Animal selectedAnimal = findAnimal(animals, animalName);

        if (selectedAnimal != null) {
            System.out.println(selectedAnimal.getName() + " is a "
                    + selectedAnimal.getDiet().getType() + ".");
            System.out.println("It makes a "
                    + selectedAnimal.makeSound() + " sound.");
        } else {
            System.out.println("Sorry, that animal is not available.");
        }

        scanner.close();
    }

    private static Animal findAnimal(Animal[] animals, String animalName) {
        for (Animal animal : animals) {
            if (animal.getName().equalsIgnoreCase(animalName)) {
                return animal;
            }
        }

        return null;
    }
}