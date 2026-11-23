// Runs the translation of `Guarded`, which names another mod's class that isn't there, and prints what it gives as
// `AbsentCheck.java` prints the Java's: the two must be the same. The translation is put in here by the test.
include!(concat!(env!("TRANSLATED_DIR"), "/translated.rs"));

fn main() {
    match guarded__all() {
        Ok(s) => print!("{}", jstring_text(&s)),
        Err(e) => println!("throws {}", jdescribe(&e)),
    }
}
