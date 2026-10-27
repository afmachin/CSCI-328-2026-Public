import java.util.List;
import java.util.Set;

/**
 * DOES NOT COMPILE, on purpose. That failure is the demonstration: a fourth node type is
 * added to the sealed interface and the existing switch is left alone.
 *
 *   javac CompositeMissingCase.java
 *
 * The recorded answer is in javac-output.txt beside this file. It is the only .java in
 * this folder, so nothing else breaks when this one does.
 */
public class CompositeMissingCase {

    record Transcript(Set<String> passed) {
        boolean hasPassed(String code) { return passed.contains(code); }
    }

    sealed interface Req {
        record Course(String code) implements Req {}
        record AllOf(List<Req> parts) implements Req {}
        record AnyOf(List<Req> parts) implements Req {}
        record MinimumGrade(String code, char grade) implements Req {}   // the new one
    }

    // Unchanged from CompositeDemo. The registrar's new requirement is not handled.
    static boolean satisfiedBy(Req req, Transcript t) {
        return switch (req) {
            case Req.Course c -> t.hasPassed(c.code());
            case Req.AllOf a  -> a.parts().stream().allMatch(p -> satisfiedBy(p, t));
            case Req.AnyOf a  -> a.parts().stream().anyMatch(p -> satisfiedBy(p, t));
        };
    }
}
