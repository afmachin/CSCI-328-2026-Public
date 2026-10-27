// DOES NOT COMPILE, on purpose. That failure is the demonstration. 
// The decorator's inner object is interchangeable with the decorator.
// The adapter's inner object is not interchangeable with the adapter.
//
//   javac AdapterNotAssignable.java
//
// The recorded answer is in javac-output.txt beside this file. It is the only .java in
// this folder, so nothing else breaks when this one does.

import java.util.Set;

public class AdapterNotAssignable {

    interface TranscriptSource { Set<String> passedCoursesFor(String id); }

    static final class LegacySisClient {
        String[][] fetchGradeRows(String id) { return new String[0][]; }
    }

    static final class AuditingTranscriptSource implements TranscriptSource {
        private final TranscriptSource inner;
        AuditingTranscriptSource(TranscriptSource inner) { this.inner = inner; }
        public Set<String> passedCoursesFor(String id) { return inner.passedCoursesFor(id); }
    }

    public static void main(String[] args) {
        TranscriptSource real = id -> Set.of();

        // Decorator: wrapper and wrapped are the same type, both ways round.
        TranscriptSource decorated = new AuditingTranscriptSource(real);
        TranscriptSource unwrapped = real;                 // fine
        System.out.println(decorated + " " + unwrapped);

        // Adapter: the wrapped object cannot stand in for the wrapper.
        LegacySisClient sis = new LegacySisClient();
        TranscriptSource notAnAdapter = sis;               // <-- the error
        System.out.println(notAnAdapter);
    }
}
