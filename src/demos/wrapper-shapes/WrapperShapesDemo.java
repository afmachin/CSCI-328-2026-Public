// What the three wrappers look like to the compiler.
//
//   cd demos/wrapper-shapes
//   javac WrapperShapesDemo.java && java WrapperShapesDemo
//
// The recorded output is in output.txt beside this file. The companion exhibit is
// reference/demos/adapter-not-assignable/, which does not compile on purpose.
// The vendor type and the domain interface are stand-ins for
// registration/LegacySisClient.java and the
// TranscriptSource interface introduced in Structural Design Module on canvas.

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class WrapperShapesDemo {

    // ---- the domain's own interface -------------------------------------
    interface TranscriptSource {
        Set<String> passedCoursesFor(String studentId);
    }

    // ---- vendor code, which speaks a different language -----------------
    static final class LegacySisClient {
        String[][] fetchGradeRows(String studentId) {
            return new String[][] { { "CS101", "B" }, { "CS150", "F" } };
        }
    }

    // ---- ADAPTER: implements TranscriptSource, holds a LegacySisClient ---
    static final class LegacySisTranscriptSource implements TranscriptSource {
        private final LegacySisClient sis;
        LegacySisTranscriptSource(LegacySisClient sis) { this.sis = sis; }
        @Override public Set<String> passedCoursesFor(String studentId) {
            Set<String> passed = new HashSet<>();
            for (String[] row : sis.fetchGradeRows(studentId)) {
                if (isPassingSisCode(row[1])) passed.add(row[0]);
            }
            return Set.copyOf(passed);
        }
        private static boolean isPassingSisCode(String code) {
            return switch (code) {
                case "A", "B", "C" -> true;
                case "D", "F", "W" -> false;
                default            -> false;
            };
        }
    }

    // ---- DECORATOR: implements TranscriptSource, holds a TranscriptSource -
    static final class AuditingTranscriptSource implements TranscriptSource {
        private final TranscriptSource inner;
        AuditingTranscriptSource(TranscriptSource inner) { this.inner = inner; }
        @Override public Set<String> passedCoursesFor(String studentId) {
            Set<String> result = inner.passedCoursesFor(studentId);
            System.out.println("    [audit] looked up " + studentId);
            return result;
        }
    }

    // ---- PROXY: implements TranscriptSource, holds a TranscriptSource -----
    static final class CachingTranscriptSource implements TranscriptSource {
        private final TranscriptSource inner;
        private Set<String> cached;
        CachingTranscriptSource(TranscriptSource inner) { this.inner = inner; }
        @Override public Set<String> passedCoursesFor(String studentId) {
            if (cached == null) cached = inner.passedCoursesFor(studentId);
            return cached;
        }
    }

    public static void main(String[] args) {
        report("Adapter  ", LegacySisTranscriptSource.class);
        report("Decorator", AuditingTranscriptSource.class);
        report("Proxy    ", CachingTranscriptSource.class);

        System.out.println();
        System.out.println("Only the adapter's wrapped type differs from the type it implements.");
        System.out.println("Decorator and Proxy are identical here. Nothing below the name tells them apart.");

        System.out.println();
        LegacySisClient sis = new LegacySisClient();
        TranscriptSource adapted = new LegacySisTranscriptSource(sis);
        TranscriptSource stack = new CachingTranscriptSource(new AuditingTranscriptSource(adapted));
        System.out.println("first  call: " + stack.passedCoursesFor("S001"));
        System.out.println("second call: " + stack.passedCoursesFor("S001"));
        System.out.println("The audit line appears once, not twice: the proxy did not delegate.");
    }

    private static void report(String label, Class<?> wrapper) {
        Field held = wrapper.getDeclaredFields()[0];
        Class<?> implemented = wrapper.getInterfaces()[0];
        System.out.println(label + "  implements " + implemented.getSimpleName()
                + ", holds a " + held.getType().getSimpleName()
                + "   -> same type? " + (implemented == held.getType()));
    }
}
