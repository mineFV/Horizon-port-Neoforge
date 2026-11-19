package java.util;

/**
 * The model of java.util.ServiceLoader, for the mod's own services: their providers are those its jar lists
 * (META-INF/services, each once, in their order; an addon's providers of them aren't known here), made lazily as the JDK makes them (observed): each the first time an
 * iterator reaches it, kept for the iterators after (a provider that couldn't be made is passed, not tried again),
 * until reload. A service of another jar's (the game's, a library's, Java's) may have providers elsewhere: refused.
 * Not modelled: stream(), Provider, the concurrent modification an iterator sees after reload.
 */
public final class ServiceLoader<S> implements Iterable<S> {
    private final Class<S> service;
    private final int count;
    private final ArrayList<S> made = new ArrayList<>();
    private int next;

    private ServiceLoader(Class<S> service) {
        this.service = service;
        this.count = service.jServiceCount();
        if (count < 0) throw new UnsupportedOperationException("the providers of ".concat(service.getName()).concat(" aren't known (horizon-port-neoforge)"));
    }

    /** loader: the mod's (null, the system's, sees none of the mod's providers: refused). */
    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        Objects.requireNonNull(service);
        if (loader == null) throw new UnsupportedOperationException("the system class loader's providers aren't modelled (horizon-port-neoforge)");
        return new ServiceLoader<>(service);
    }

    /** The thread's context loader: in NeoForge, the one that loaded the mod. */
    public static <S> ServiceLoader<S> load(Class<S> service) {
        Objects.requireNonNull(service);
        return new ServiceLoader<>(service);
    }

    public Iterator<S> iterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < made.size() || next < count;
            }

            @Override
            public S next() {
                if (index < made.size()) return made.get(index++);
                if (next >= count) throw new NoSuchElementException();
                int i = next++;
                Object provider;
                try {
                    provider = service.jServiceMake(i);
                } catch (Throwable t) {
                    throw new ServiceConfigurationError(service.getName().concat(": Provider ").concat(service.jServiceName(i)).concat(" could not be instantiated"), t);
                }
                S s = service.cast(provider);
                made.add(s);
                index++;
                return s;
            }
        };
    }

    public Optional<S> findFirst() {
        Iterator<S> it = iterator();
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }

    public void reload() {
        made.clear();
        next = 0;
    }

    public String toString() {
        return "java.util.ServiceLoader[".concat(service.getName()).concat("]");
    }
}
