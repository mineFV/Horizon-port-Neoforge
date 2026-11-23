/** Runs `Guarded`, which names another mod's class that isn't there, as `absent_check.rs` runs its translation. */
public final class AbsentCheck {
    public static void main(String[] args) {
        System.out.print(com.example.absent.Guarded.all());
    }
}
