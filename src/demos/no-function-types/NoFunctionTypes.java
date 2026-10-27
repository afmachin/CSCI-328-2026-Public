/**
 * DOES NOT COMPILE, on purpose. That failure is the demonstration, and there are two of
 * them, both from note 08.
 *
 *   javac NoFunctionTypes.java
 *
 * The recorded answer is in javac-output.txt beside this file. It is the only .java in
 * this folder, so nothing else breaks when this one does.
 */
public class NoFunctionTypes {

    // Two interfaces. One abstract method each. Identical signature.
    interface WaitlistPolicy { int rank(String studentId); }
    interface Priority       { int rank(String studentId); }

    static void sameShapeIsNotTheSameType() {
        WaitlistPolicy policy = id -> id.length();
        Priority priority = policy;              // (1) same shape, so surely interchangeable?
        System.out.println(priority.rank("S001"));
    }

    static void aLambdaHasNoTypeOfItsOwn() {
        var ranker = (String id) -> id.length();  // (2) what type is this, on its own?
        System.out.println(ranker);
    }
}
