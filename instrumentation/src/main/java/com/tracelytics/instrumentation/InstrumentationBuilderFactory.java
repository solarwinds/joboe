package com.tracelytics.instrumentation;

public class InstrumentationBuilderFactory {
    public static final InstrumentationBuilderFactory INSTANCE = new InstrumentationBuilderFactory();

    private InstrumentationBuilderFactory() {
    }

    <T> InstrumentationBuilder<T> getBuilder(Class<? extends T> instrumentationClass)  {
        return new SimpleInstrumentationBuilder<T>(instrumentationClass);
    }

    /**
     * Simple instrumentation builder that builds an instrumentation by invoking default constructor of the provided instrumentationClass
     * @author pluk
     *
     * @param <T>
     */
    static class SimpleInstrumentationBuilder<T> implements InstrumentationBuilder<T> {
        private final Class<? extends T> instrumentationClass;
        private SimpleInstrumentationBuilder(Class<? extends T> instrumentationClass) {
            this.instrumentationClass = instrumentationClass;
        }

        @Override
        public T build() throws InstantiationException, IllegalAccessException {
            return instrumentationClass.newInstance();
        }

        @Override
        public String toString() {
            return instrumentationClass.getName();
        }

        public Class<? extends T> getInstrumentationClass() {
            return instrumentationClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleInstrumentationBuilder<?> that = (SimpleInstrumentationBuilder<?>) o;
            return instrumentationClass.equals(that.instrumentationClass);
        }

        @Override
        public int hashCode() {
            return instrumentationClass.hashCode();
        }
    }
}


