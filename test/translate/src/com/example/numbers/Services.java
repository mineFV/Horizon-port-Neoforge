package com.example.numbers;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/** ServiceLoader over the jar's own services file (test/translate's run-test.sh writes it): the JDK's order, laziness and errors. */
public final class Services {
    private Services() {}

    public interface Greeter {
        String hi();
    }

    public interface Nobody {}

    public static final class Plain implements Greeter {
        static int made;

        public Plain() {
            made++;
        }

        @Override
        public String hi() {
            return "plain" + made;
        }
    }

    public static final class Broken implements Greeter {
        public Broken() {
            throw new IllegalStateException("boom");
        }

        @Override
        public String hi() {
            return "broken";
        }
    }

    public static final class Last implements Greeter {
        @Override
        public String hi() {
            return "last";
        }
    }

    public static String report() {
        StringBuilder b = new StringBuilder();
        ServiceLoader<Greeter> l = ServiceLoader.load(Greeter.class, Services.class.getClassLoader());
        b.append(l).append(';');
        Iterator<Greeter> it = l.iterator();
        b.append(it.hasNext()).append(' ').append(Plain.made).append(';');
        Greeter a = it.next();
        b.append(a.hi()).append(' ').append(Plain.made).append(' ').append(it.hasNext()).append(';');
        try {
            it.next();
        } catch (ServiceConfigurationError e) {
            b.append(e.getMessage()).append(" | ").append(e.getCause()).append(';');
        }
        b.append(it.hasNext()).append(' ').append(it.next().hi()).append(' ').append(it.hasNext()).append(';');
        Iterator<Greeter> again = l.iterator();
        b.append(again.next() == a).append(' ').append(again.next().hi()).append(' ').append(again.hasNext()).append(' ').append(Plain.made).append(';');
        l.reload();
        b.append(l.iterator().next() == a).append(' ').append(Plain.made).append(' ').append(l.findFirst().map(Greeter::hi).orElse("none")).append(';');
        ServiceLoader<Nobody> none = ServiceLoader.load(Nobody.class, Services.class.getClassLoader());
        b.append(none.iterator().hasNext()).append(' ').append(none.findFirst().isPresent()).append(' ').append(none);
        return b.toString();
    }

    public static String loaders() {
        return (Services.class.getClassLoader() == Greeter.class.getClassLoader()) + " " + (Services.class.getClassLoader() != null) + " "
                + (Services[].class.getClassLoader() == Services.class.getClassLoader()) + " " + String.class.getClassLoader() + " " + int[].class.getClassLoader()
                + " " + int.class.getClassLoader() + " " + (Runnable.class.getClassLoader() == null);
    }
}
