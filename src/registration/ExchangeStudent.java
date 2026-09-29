package registration;

/** Exchange records must not collide with the home institution's ids,
 * so the term is prefixed. */
public class ExchangeStudent extends Student {
    private final String term;

    public ExchangeStudent(String id, String name, String term) {
        super(id, name, "UNDERGRADUATE");
        this.term = term;
    }

    @Override
    public String getId() {
        return term + "-" + super.getId();
    }
}